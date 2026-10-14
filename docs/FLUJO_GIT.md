# Flujo de ramas

Usa `main` para las versiones de producción y `test` para integrar cambios que aún deben probarse.

## Primera publicación

Después del primer commit, publica la rama `main` y crea `test` desde ella:

```bash
git branch -M main
git add .
git commit -m "versión inicial v1.0.0"
git push -u origin main
git switch -c test
git push -u origin test
```

Si el primer commit ya existe en `master`, cambia su nombre a `main` antes de ejecutar los comandos.

## Trabajo habitual

1. Crea una rama corta desde `test` para cada cambio: `feature/nombre` o `fix/nombre`.
2. Integra el cambio en `test` y valida la app en el entorno de pruebas.
3. Cuando esté listo para producción, integra `test` en `main` mediante un Pull Request.
4. Marca cada versión publicada con una etiqueta, por ejemplo `v1.0.0`.

```bash
git switch test
git switch -c feature/nombre
# realiza cambios y commits
git push -u origin feature/nombre
```

Mantén credenciales de prueba y producción en proyectos Supabase separados. La clave `publishable` del cliente puede estar en el código; nunca agregues claves `secret`/`service_role`, contraseñas ni archivos de firma.
