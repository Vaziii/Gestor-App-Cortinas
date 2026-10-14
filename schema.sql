-- Base de datos de Cortinas para PostgreSQL / Supabase.
-- Ejecutar en el SQL Editor del proyecto Supabase.
-- Las políticas requieren sesión Supabase Auth; no se habilita acceso público anónimo.

begin;

create table if not exists public.telas (
    id text primary key,
    modelo text not null,
    tipo text not null default 'Translúcida',
    precio_m2 numeric(12,2) not null default 0 check (precio_m2 >= 0),
    detalles text not null default '',
    apto_impresion boolean,
    apto_exterior boolean,
    ancho_catalogo text not null default '',
    catalogo_origen text not null default '',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
   b
alter table public.telas add column if not exists apto_impresion boolean;
alter table public.telas add column if not exists apto_exterior boolean;
alter table public.telas add column if not exists ancho_catalogo text not null default '';
alter table public.telas add column if not exists catalogo_origen text not null default '';
alter table public.telas add column if not exists updated_at timestamptz not null default now();

create table if not exists public.clientes (
    id text primary key,
    nombre text not null,
    telefono text not null default '',
    direccion text not null default '',
    notas text not null default '',
    fecha_registro bigint not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.cotizaciones (
    id text primary key,
    codigo text not null default '',
    cliente_id text not null default '',
    -- Se conserva una copia de los datos del cliente para que la cotización
    -- histórica no cambie si luego se edita o elimina el cliente.
    cliente_nombre text not null default '',
    cliente_telefono text not null default '',
    cliente_direccion text not null default '',
    fecha bigint not null,
    costo_instalacion numeric(12,2) not null default 0 check (costo_instalacion >= 0),
    descuento numeric(12,2) not null default 0 check (descuento >= 0),
    estado text not null default 'COTIZACION'
        check (estado in ('COTIZACION', 'VENDIDA', 'EN_PROCESO', 'INSTALADA', 'CANCELADA')),
    notas text not null default '',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

alter table public.clientes add column if not exists updated_at timestamptz not null default now();
alter table public.cotizaciones add column if not exists updated_at timestamptz not null default now();

create table if not exists public.cotizacion_items (
    id text primary key,
    cotizacion_id text not null references public.cotizaciones(id) on delete cascade,
    ambiente text not null default 'Ventana',
    ancho numeric(10,3) not null default 1 check (ancho > 0),
    alto numeric(10,3) not null default 1 check (alto > 0),
    es_dos_cuerpos boolean not null default false,
    ancho_cuerpo_izq numeric(10,3) not null default 0 check (ancho_cuerpo_izq >= 0),
    ancho_cuerpo_der numeric(10,3) not null default 0 check (ancho_cuerpo_der >= 0),
    tela_id text,
    tela_modelo text not null default '',
    tela_tipo text not null default 'Translúcida',
    precio_m2 numeric(12,2) not null default 0 check (precio_m2 >= 0),
    costo_motor numeric(12,2) not null default 0 check (costo_motor >= 0),
    costo_extras numeric(12,2) not null default 0 check (costo_extras >= 0),
    mando_lado text not null default 'Derecha',
    caida text not null default 'Normal',
    notas text not null default '',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.abonos (
    id text primary key,
    cotizacion_id text not null references public.cotizaciones(id) on delete cascade,
    monto numeric(12,2) not null check (monto > 0),
    fecha bigint not null,
    metodo_pago text not null default 'Efectivo',
    referencia text not null default '',
    notas text not null default '',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- Metadatos públicos de versiones; el APK debe servirse desde un enlace HTTPS.
create table if not exists public.app_updates (
    platform text not null,
    version_code integer not null check (version_code > 0),
    version_name text not null,
    apk_url text not null check (apk_url like 'https://%'),
    mandatory boolean not null default false,
    release_notes text not null default '',
    sha256 text not null default '',
    primary key (platform, version_code)
);
alter table public.app_updates enable row level security;
revoke all on table public.app_updates from public, anon, authenticated;
grant select on table public.app_updates to anon, authenticated;
drop policy if exists "Lectura pública de actualizaciones" on public.app_updates;
create policy "Lectura pública de actualizaciones" on public.app_updates
    for select to anon, authenticated using (true);

-- Evita registrar cobros que superen el total, incluso con solicitudes simultáneas.
create or replace function public.validar_abono_no_exceda_saldo()
returns trigger
language plpgsql
set search_path = public
as $$
declare
    v_instalacion numeric;
    v_descuento numeric;
    v_total numeric;
    v_abonado numeric;
begin
    select costo_instalacion, descuento
    into v_instalacion, v_descuento
    from public.cotizaciones
    where id = new.cotizacion_id
    for update;

    if not found then
        raise exception 'No existe la cotización para este abono.';
    end if;

    select greatest(0, coalesce(sum(ancho * alto * precio_m2 + costo_motor + costo_extras), 0)
        + v_instalacion - v_descuento)
    into v_total
    from public.cotizacion_items
    where cotizacion_id = new.cotizacion_id;

    select coalesce(sum(monto), 0)
    into v_abonado
    from public.abonos
    where cotizacion_id = new.cotizacion_id
      and id <> new.id;

    if v_abonado + new.monto > v_total then
        raise exception 'El abono supera el saldo pendiente.';
    end if;
    return new;
end;
$$;

drop trigger if exists abonos_validar_saldo on public.abonos;
create trigger abonos_validar_saldo
before insert or update of monto, cotizacion_id on public.abonos
for each row execute function public.validar_abono_no_exceda_saldo();

-- Guarda cabecera, ítems y abonos en una sola transacción y asigna un código
-- consecutivo al guardar, serializando la asignación entre dispositivos.
create or replace function public.guardar_cotizacion_completa(
    p_cotizacion jsonb,
    p_items jsonb,
    p_abonos jsonb
)
returns jsonb
language plpgsql
security invoker
set search_path = public
as $$
declare
    v_codigo text := nullif(btrim(p_cotizacion->>'codigo'), '');
    v_siguiente bigint;
    v_fila jsonb;
begin
    if v_codigo is null then
        select nullif(codigo, '')
        into v_codigo
        from public.cotizaciones
        where id = p_cotizacion->>'id';
    end if;

    if v_codigo is null then
        perform pg_advisory_xact_lock(hashtext('cortinas:codigo-cotizacion'));
        -- Una petición anterior con el mismo ID pudo confirmar mientras esta esperaba.
        select nullif(codigo, '') into v_codigo
        from public.cotizaciones
        where id = p_cotizacion->>'id';
        if v_codigo is null then
            select coalesce(max(substring(codigo from 5)::bigint), 0) + 1
            into v_siguiente
            from public.cotizaciones
            where codigo ~ '^COT-[0-9]+$';
            v_codigo := 'COT-' || lpad(v_siguiente::text, 4, '0');
        end if;
    end if;

    insert into public.cotizaciones (
        id, codigo, cliente_id, cliente_nombre, cliente_telefono, cliente_direccion,
        fecha, costo_instalacion, descuento, estado, notas, updated_at
    )
    select r.id, v_codigo, r.cliente_id, r.cliente_nombre, r.cliente_telefono, r.cliente_direccion,
        r.fecha, r.costo_instalacion, r.descuento, r.estado, r.notas, r.updated_at
    from jsonb_populate_record(null::public.cotizaciones, p_cotizacion) as r
    on conflict (id) do update set
        codigo = excluded.codigo,
        cliente_id = excluded.cliente_id,
        cliente_nombre = excluded.cliente_nombre,
        cliente_telefono = excluded.cliente_telefono,
        cliente_direccion = excluded.cliente_direccion,
        fecha = excluded.fecha,
        costo_instalacion = excluded.costo_instalacion,
        descuento = excluded.descuento,
        estado = excluded.estado,
        notas = excluded.notas,
        updated_at = excluded.updated_at;

    for v_fila in select value from jsonb_array_elements(coalesce(p_items, '[]'::jsonb))
    loop
        insert into public.cotizacion_items (
            id, cotizacion_id, ambiente, ancho, alto, es_dos_cuerpos,
            ancho_cuerpo_izq, ancho_cuerpo_der, tela_id, tela_modelo, tela_tipo,
            precio_m2, costo_motor, costo_extras, mando_lado, caida, notas
        )
        select r.id, r.cotizacion_id, r.ambiente, r.ancho, r.alto, r.es_dos_cuerpos,
            r.ancho_cuerpo_izq, r.ancho_cuerpo_der, r.tela_id, r.tela_modelo, r.tela_tipo,
            r.precio_m2, r.costo_motor, r.costo_extras, r.mando_lado, r.caida, r.notas
        from jsonb_populate_record(null::public.cotizacion_items, v_fila) as r
        on conflict (id) do update set
            cotizacion_id = excluded.cotizacion_id,
            ambiente = excluded.ambiente,
            ancho = excluded.ancho,
            alto = excluded.alto,
            es_dos_cuerpos = excluded.es_dos_cuerpos,
            ancho_cuerpo_izq = excluded.ancho_cuerpo_izq,
            ancho_cuerpo_der = excluded.ancho_cuerpo_der,
            tela_id = excluded.tela_id,
            tela_modelo = excluded.tela_modelo,
            tela_tipo = excluded.tela_tipo,
            precio_m2 = excluded.precio_m2,
            costo_motor = excluded.costo_motor,
            costo_extras = excluded.costo_extras,
            mando_lado = excluded.mando_lado,
            caida = excluded.caida,
            notas = excluded.notas;
    end loop;

    for v_fila in select value from jsonb_array_elements(coalesce(p_abonos, '[]'::jsonb))
    loop
        insert into public.abonos (
            id, cotizacion_id, monto, fecha, metodo_pago, referencia, notas
        )
        select r.id, r.cotizacion_id, r.monto, r.fecha, r.metodo_pago, r.referencia, r.notas
        from jsonb_populate_record(null::public.abonos, v_fila) as r
        on conflict (id) do update set
            cotizacion_id = excluded.cotizacion_id,
            monto = excluded.monto,
            fecha = excluded.fecha,
            metodo_pago = excluded.metodo_pago,
            referencia = excluded.referencia,
            notas = excluded.notas;
    end loop;

    return jsonb_build_object('codigo', v_codigo);
end;
$$;

revoke all on function public.guardar_cotizacion_completa(jsonb, jsonb, jsonb) from public, anon;
grant execute on function public.guardar_cotizacion_completa(jsonb, jsonb, jsonb) to authenticated;

-- Compatibilidad con instalaciones creadas usando el esquema anterior.
alter table public.cotizacion_items add column if not exists es_dos_cuerpos boolean not null default false;
alter table public.cotizacion_items add column if not exists ancho_cuerpo_izq numeric(10,3) not null default 0;
alter table public.cotizacion_items add column if not exists ancho_cuerpo_der numeric(10,3) not null default 0;

-- Índices para listar cotizaciones y consultar sus componentes rápidamente.
create index if not exists idx_cotizaciones_fecha on public.cotizaciones (fecha desc);
create index if not exists idx_cotizaciones_cliente on public.cotizaciones (cliente_id);
create index if not exists idx_cotizaciones_estado on public.cotizaciones (estado);
create index if not exists idx_items_cotizacion on public.cotizacion_items (cotizacion_id);
create index if not exists idx_abonos_cotizacion on public.abonos (cotizacion_id);

-- RLS restricts all app data to explicitly authorized business members.
create table if not exists public.business_members (
    user_id uuid primary key references auth.users(id) on delete cascade,
    active boolean not null default true,
    created_at timestamptz not null default now()
);

alter table public.business_members enable row level security;
revoke all on table public.business_members from anon, authenticated, public;
grant select on table public.business_members to authenticated;
do $$
declare
    existing_policy record;
begin
    for existing_policy in
        select policyname from pg_policies
        where schemaname = 'public' and tablename = 'business_members'
    loop
        execute format('drop policy if exists %I on public.business_members', existing_policy.policyname);
    end loop;
end $$;
drop policy if exists "Usuarios consultan su membresia" on public.business_members;
create policy "Usuarios consultan su membresia" on public.business_members
    for select to authenticated using (user_id = (select auth.uid()) and active);

do $$
declare
    existing_policy record;
begin
    for existing_policy in
        select schemaname, tablename, policyname
        from pg_policies
        where schemaname = 'public'
          and tablename = any (array['telas', 'clientes', 'cotizaciones', 'cotizacion_items', 'abonos'])
    loop
        execute format('drop policy if exists %I on %I.%I',
            existing_policy.policyname, existing_policy.schemaname, existing_policy.tablename);
    end loop;
end $$;

alter table public.telas enable row level security;
alter table public.clientes enable row level security;
alter table public.cotizaciones enable row level security;
alter table public.cotizacion_items enable row level security;
alter table public.abonos enable row level security;

revoke all on table public.telas, public.clientes, public.cotizaciones,
    public.cotizacion_items, public.abonos from anon, public;
grant select, insert, update, delete on table public.telas, public.clientes,
    public.cotizaciones, public.cotizacion_items, public.abonos to authenticated;

create policy "Miembros autorizados gestionan telas" on public.telas
    for all to authenticated
    using (exists (select 1 from public.business_members m where m.user_id = (select auth.uid()) and m.active))
    with check (exists (select 1 from public.business_members m where m.user_id = (select auth.uid()) and m.active));
create policy "Miembros autorizados gestionan clientes" on public.clientes
    for all to authenticated
    using (exists (select 1 from public.business_members m where m.user_id = (select auth.uid()) and m.active))
    with check (exists (select 1 from public.business_members m where m.user_id = (select auth.uid()) and m.active));
create policy "Miembros autorizados gestionan cotizaciones" on public.cotizaciones
    for all to authenticated
    using (exists (select 1 from public.business_members m where m.user_id = (select auth.uid()) and m.active))
    with check (exists (select 1 from public.business_members m where m.user_id = (select auth.uid()) and m.active));
create policy "Miembros autorizados gestionan items" on public.cotizacion_items
    for all to authenticated
    using (exists (select 1 from public.business_members m where m.user_id = (select auth.uid()) and m.active))
    with check (exists (select 1 from public.business_members m where m.user_id = (select auth.uid()) and m.active));
create policy "Miembros autorizados gestionan abonos" on public.abonos
    for all to authenticated
    using (exists (select 1 from public.business_members m where m.user_id = (select auth.uid()) and m.active))
    with check (exists (select 1 from public.business_members m where m.user_id = (select auth.uid()) and m.active));

commit;

notify pgrst, 'reload schema';
