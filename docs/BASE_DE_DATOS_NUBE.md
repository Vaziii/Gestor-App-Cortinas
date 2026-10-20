# Base de datos en la nube y acceso seguro

La app usa Supabase Auth para iniciar sesión y PostgreSQL para almacenar la información del negocio. La clave `publishable` incluida en Android es pública por diseño; la protección real está en RLS y en la lista privada `public.business_members`.

## Configuración requerida en Supabase

1. En **Authentication → Providers → Email**, desactiva los registros públicos. No habilites el acceso anónimo.
2. Crea tu usuario desde **Authentication → Users → Add user**. Usa una contraseña única y activa MFA si está disponible para tu proyecto.
3. Abre **SQL Editor** y ejecuta una vez el archivo completo `schema.sql`. El script habilita RLS, quita las políticas previas de las tablas de la app y reemplaza el acceso general de usuarios autenticados por una lista de miembros autorizados.
4. Autoriza tu cuenta. Ejecuta esta consulta sustituyendo el correo por el de tu usuario de Supabase:

   ```sql
   insert into public.business_members (user_id)
   select id from auth.users where lower(email) = lower('tu-correo@ejemplo.com')
   on conflict (user_id) do update set active = true;
   ```

   Comprueba que se insertó una fila:

   ```sql
   select m.user_id, u.email, m.active
   from public.business_members m
   join auth.users u on u.id = m.user_id;
   ```

   Si no aparece ninguna fila, revisa que el usuario exista y que el correo coincida antes de abrir la app.

5. En **Authentication → Settings**, confirma que el registro está deshabilitado y revisa los límites de intentos de acceso. No compartas la contraseña entre trabajadores: autoriza cada cuenta individual insertando su UUID en `business_members`; desautoriza una cuenta con `update public.business_members set active = false where user_id = 'UUID';`.

La tabla `business_members` no permite que la app agregue ni active usuarios. Solo un administrador de Supabase puede cambiar esa lista desde SQL Editor o el panel. Las políticas de datos requieren una fila activa para leer o modificar telas, clientes, cotizaciones, ítems y abonos.

El mismo script crea `public.app_updates`, una tabla de solo lectura pública para que la app consulte versiones antes del inicio de sesión. El administrador debe publicar ahí cada versión Android con su URL HTTPS y SHA-256; las escrituras se hacen desde el panel o SQL Editor, nunca desde la app.

## Comportamiento en Android

- La app pide y valida sesión al iniciar. Si no puede validar o sincronizar con Supabase, mantiene bloqueadas las pantallas con datos locales.
- Al cerrar sesión, vencer la sesión o revocarse el acceso, la app vuelve a la pantalla de acceso.
- Los tokens se cifran con AES-GCM usando una clave del Android Keystore. La configuración migra los tokens guardados por versiones anteriores al leerlos.
- Android Auto Backup y la transferencia de datos excluyen la base local y las preferencias. La app bloquea capturas de pantalla y vistas previas en Recientes.
- La base local sigue dentro del sandbox privado de Android; esta protección no cubre un dispositivo rooteado o comprometido. Desinstalar la app elimina también la caché local.

## Mantenimiento

- No incluyas contraseñas de PostgreSQL ni claves `service_role`/secretas en Android, en el repositorio ni en capturas.
- Si ejecutas de nuevo `schema.sql`, las políticas de las cinco tablas de la app se reemplazan por las políticas seguras. El script elimina también políticas personalizadas que hayas creado en esas tablas; revisa ese efecto antes de ejecutarlo si agregaste políticas propias.
- El guardado de una cotización usa la función `guardar_cotizacion_completa` para escribir cabecera, ítems y abonos en una sola transacción. El código consecutivo se asigna en el servidor al guardar y termina con la primera letra del correo de la sesión (por ejemplo, `COT-0001m`); el botón de WhatsApp antes de guardar comparte un borrador sin número. Al desplegar cambios de `schema.sql`, vuelve a ejecutar ese archivo en el SQL Editor de Supabase para actualizar la función.
- Si una cuenta autorizada se pierde o deja de usarse, desactívala en `business_members` y elimina/revoca sus sesiones desde **Authentication → Users**.
