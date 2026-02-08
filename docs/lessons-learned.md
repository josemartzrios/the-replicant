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

**Document Version**: 1.0  
**Created**: 2026-01-21  
**Last Updated**: 2026-01-21
