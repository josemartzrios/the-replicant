# Lessons Learned

> Lecciones prácticas aprendidas durante el desarrollo de The Replicant.

---

## 📊 Database Design

### 1. Relaciones Many-to-Many requieren tabla intermedia
```
POST ──N:M──► TAG   requiere   POST_TAG (junction table)
```

### 2. UUIDs vs Auto-increment
- UUIDs no exponen cuántos registros hay (`/users/5` → revela info) autoincrement si

### 3. Soft Delete con `deleted_at`
- No borras realmente, marcas con timestamp
- Permite recuperar contenido eliminado
- Queries filtran con `WHERE deleted_at IS NULL`

### 4. Auditoría con timestamps
```sql
created_at TIMESTAMP DEFAULT NOW()
updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
```

### 5. Self-Referencing para jerarquías
```sql
users.created_by → users.id  -- Admin crea Authors
```

### 6. Tokens hasheados, nunca en texto plano
- Guardas `token_hash`, no el token real
- Si roban la DB, no pueden usar los tokens

### 7. Expiración + Revocación de tokens
- `expires_at` → vida limitada
- `revoked_at` → invalidación manual (logout)

### 8. Índices parciales
```sql
CREATE INDEX idx_posts_status ON posts(status) WHERE deleted_at IS NULL;
```

### 9. Cuándo crear índices

No pones índice en TODO. Solo en columnas que:
- Se usan mucho en `WHERE`
- Se usan en `ORDER BY`
- Se usan en `JOIN`

| Sin índice | Con índice |
|------------|------------|
| Buscar en un libro leyendo cada página | Ir directo al índice del final y encontrar la página |
| Buscar tu contacto revisando TODO el celular | Buscar por la letra "M" en tu agenda |
| Revisar 10,000 filas una por una | Saltar directamente a las 5 que necesitas |


### 10. ENUMs para estados finitos
```sql
status ENUM('DRAFT', 'PUBLISHED')  -- DB valida valores
```

### 11. Enums en Java: Mapeo de campos ENUM de la base de datos

**¿Qué es un Enum en Java?**  
Un `enum` es un tipo de dato especial que representa un conjunto **fijo** de constantes. Es como una lista cerrada de opciones válidas.

```java
// Role.java - NO es una tabla, es un enum Java
public enum Role {
    ADMIN,    // Constante 1
    AUTHOR    // Constante 2
}
```

**¿Por qué crear una clase Enum para campos ENUM de la BD?**

| Sin Enum Java | Con Enum Java |
|---------------|---------------|
| `user.setRole("admin")` ← Puede haber typos | `user.setRole(Role.ADMIN)` ← Autocompletado |
| `if (role.equals("ADMIN"))` ← Error-prone | `if (role == Role.ADMIN)` ← Type-safe |
| No hay validación en compile time | Error de compilación si usas valor inválido |

**Cómo se mapea a la base de datos:**
```java
// En User.java
@Enumerated(EnumType.STRING)  // Guarda "ADMIN" o "AUTHOR" como texto
@Column(nullable = false)
private Role role;
```

| Enfoque | En la BD almacena | Pros/Cons |
|---------|-------------------|-----------|
| `EnumType.STRING` | `"ADMIN"` (texto) | ✅ Legible, seguro si renombras |
| `EnumType.ORDINAL` | `0`, `1` (índice) | ❌ Se rompe si reordenas el enum |

**Regla:** Cuando tu esquema de BD tiene un campo `ENUM`, crea un `enum` Java correspondiente para tener validación en tiempo de compilación y evitar errores de typos.

---

## 🔀 Git Workflows

### 1. No trackear archivos sensibles
```bash
# Agregar a .gitignore ANTES de hacer commit
echo ".env" >> .gitignore
echo "node_modules/" >> .gitignore

# Si ya trackeaste algo por error:
git rm --cached .env              # Remueve del tracking, mantiene el archivo
git commit -m "Stop tracking .env"
```

**⚠️ ¿Qué NUNCA debes subir a GitHub?**
- Tokens JWT reales (Hacerse pasar por ti o cualquier usuario sin necesitar contraseña. Accede al dashboard admin, borra posts, modifica datos.)
- Contraseñas de producción (Entrar directamente a tu cuenta, a la base de datos, o a cualquier servicio donde uses esa contraseña.)
- Archivos `.env` con secrets (Tiene TODAS las llaves: database URL, JWT secret, API keys. Es como entregar las llaves de tu casa.)
- API keys de servicios externos (Usar tus servicios a tu nombre (y a tu costo). Ejemplo: si subes una API key de AWS, pueden crear servidores y te llega la factura de miles de dólares.)

Scripts de prueba con datos de desarrollo (localhost, credenciales genéricas) **SÍ son seguros** de subir.

### 2. Sincronizar antes de trabajar
```bash
# SIEMPRE antes de hacer cambios:
git pull origin main

# Si ya tienes cambios locales sin commit:
git stash                   # Guarda temporalmente tus cambios
git pull origin main        # Trae cambios remotos
git stash pop               # Recupera tus cambios encima
```

### 3. Limpiar sesión de trabajo
```bash
# Al terminar de trabajar, SIEMPRE:
git add .
git commit -m "feat: descripción clara del cambio"
git push origin main

# Esto evita:
# - Perder trabajo si algo falla
# - Conflictos grandes acumulados
# - Olvidar qué hiciste
```

---

## ⚡ Rendering Strategies (Next.js)

| Estrategia | Qué hace |
|------------|----------|
| **SSG** (Static Site Generation) | Genera HTML en **build time**, sirve páginas pre-renderizadas como archivos estáticos. |
| **ISR** (Incremental Static Regeneration) | SSG + **revalidación automática** después de N segundos sin rebuild completo. |

---

## 🧪 Unit Testing: Definir Alcance y Escenarios

### 1. Cada método público es una unidad

> **Cada método público del servicio es una unidad que debe testearse.**

No testeas métodos privados directamente. Los privados se cubren indirectamente al testear los públicos que los usan.

```
AuthService tiene 5 métodos públicos:
├── checkStatus()    → 2 tests
├── setup()          → 4 tests
├── login()          → 4 tests
├── refreshToken()   → 5 tests
└── logout()         → 2 tests
Total: 17 tests
```

### 2. ¿Qué puede pasar? (Definir escenarios)

> **La pregunta clave por cada método es: ¿Qué puede pasar?**

Para cada método, dibuja un árbol de decisiones:

```
login(email, password)
├── ¿Existe el usuario?
│     ├── NO → BadCredentialsException
│     └── SI → ¿Password correcto?
│               ├── NO → BadCredentialsException
│               └── SI → Retornar tokens ✅
└── Cada rama = al menos 1 test
```

### 3. Fórmula para tipos de test

| Tipo | ¿Qué valida? | Ejemplo |
|------|---------------|---------|
| **Happy path** | Funciona correctamente | Login exitoso retorna tokens |
| **Guard clauses** | Validaciones iniciales | Setup falla si ya hay usuarios |
| **Excepciones** | Falla de forma segura | Password incorrecto lanza excepción |
| **Seguridad** | Reglas OWASP | Mismo mensaje para user-not-found y wrong-password |
| **Edge cases** | Datos límite | Logout sin tokens no crash |

**Regla:** Cada `if`, `throw`, o rama de decisión → al menos 1 test.

### 4. Unit Test vs Integration Test

> **El Unit Test verifica que cada pieza funciona aislada. El Integration Test verifica que todas las piezas funcionan juntas, desde el HTTP request hasta la base de datos.**

| | Unit Test | Integration Test |
|---|---|---|
| **Dependencias** | Mockeadas (Mockito) | Reales (Spring Boot levantado) |
| **Base de datos** | No hay | H2 en memoria |
| **HTTP** | No hay | MockMvc (requests reales) |
| **Velocidad** | Milisegundos | Segundos |
| **Valida** | Lógica de negocio aislada | Que todo funcione junto (JSON, validación, seguridad, BD) |

**MockMvc:** La única parte simulada en un integration test es la capa HTTP. En lugar de abrir un puerto como `localhost:8080`, MockMvc pasa la petición directo al controller dentro de la JVM. Todo lo demás (service, repository, BD) es real.

**H2:** Es una base de datos relacional que corre en memoria dentro de Java. Se usa como sustituto de PostgreSQL en tests: se crea automáticamente al iniciar el test, Spring JPA genera las tablas desde las `@Entity`, y la BD desaparece al terminar. No necesitas instalar nada externo.

### 5. Testea comportamiento, no implementación

> **Un buen test valida comportamiento, no implementación.**

**¿Qué significa?** Un test debe verificar **qué efecto produce** tu código (lo que el usuario o sistema experimenta), no **cómo lo hace internamente** (los detalles técnicos).

**Ejemplo real (token rotation):**

```
❌ Test de implementación (frágil):
   "El nuevo refresh token debe ser un string diferente al anterior"
   → Compara strings internos. Si el algoritmo genera el mismo string
     por timing, el test falla aunque la rotación funcione bien.

✅ Test de comportamiento (robusto):
   "Después de hacer refresh, el token anterior ya no funciona"
   → Verifica el EFECTO real: el viejo se revocó.
     No importa cómo se ve el nuevo, importa que el viejo murió.
```

**¿Por qué importa?**
- Los tests de implementación se rompen cuando refactorizas el código interno (aunque funcione igual).
- Los tests de comportamiento solo se rompen cuando el sistema deja de funcionar correctamente.
- Un test frágil que falla sin razón genera ruido → te acostumbras a ignorar el rojo → un bug real pasa desapercibido.

**Regla:** Pregúntate "¿qué le importa al usuario/sistema?" y testea eso.

---

## 🌐 cURL (Client URL)

**cURL** = Client URL. Es una herramienta de línea de comandos para enviar peticiones HTTP (y otros protocolos) desde la terminal. Piénsalo como un navegador sin interfaz visual. Es la forma más directa de hablarle a un servidor HTTP. Lo que Postman hace con botones, cURL lo hace con texto.

---

**Document Version**: 1.3  
**Created**: 2026-01-21  
**Last Updated**: 2026-02-11
