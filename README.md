# Sistema de Gestión de Inventario

Sistema full-stack con **Java 17 + Spring Boot 3 + MySQL** y **Angular 18**.

---

## 📋 Tabla de Contenido

1. [Requisitos Previos](#requisitos-previos)
2. [Instalación y Ejecución](#instalación-y-ejecución)
3. [Estructura del Proyecto](#estructura-del-proyecto)
4. [Funcionalidades](#funcionalidades)
5. [Credenciales de Acceso](#credenciales-de-acceso)
6. [Preguntas de Conocimientos](#preguntas-de-conocimientos)

---

## 🔧 Requisitos Previos

- **Java 17** o superior
- **Maven 3.6+**
- **Node.js 18+** y **pnpm**
- **MySQL 8.0+**
- **Docker y Docker Compose** (opcional)

---

## 🚀 Instalación y Ejecución

### Opción 1: Con Docker (Recomendado)

1. **Clonar el repositorio**
```bash
git clone https://github.com/johssalinas/Prueba_Tecnica_Inti_Johs_Salinas.git
cd Prueba_Tecnica_Inti_Johs_Salinas
```

2. **Levantar todos los servicios**
```bash
docker-compose up --build
```

3. **Esperar a que los servicios estén listos**
   - MySQL iniciará primero y ejecutará `db/schema.sql`
   - Backend esperará a que MySQL esté saludable
   - Frontend se construirá y servirá con Nginx

4. **Acceder a las aplicaciones**
   - Backend: http://localhost:8080
   - Frontend: http://localhost:4200

---

### Opción 2: Sin Docker (Manual)

#### 1. Base de Datos

```bash
# Iniciar MySQL
mysql -u root -p

# Crear la base de datos y ejecutar el schema
mysql -u root -p < db/schema.sql
```

O manualmente:
```sql
CREATE DATABASE inventario_db;
USE inventario_db;
SOURCE db/schema.sql;
```

#### 2. Backend

```bash
# Navegar a la carpeta backend
cd backend

# Copiar el archivo de variables de entorno
cp .env.example .env

# Ejecutar el backend
./mvnw spring-boot:run
```

El backend estará disponible en: http://localhost:8080

**Nota:** Asegúrate de configurar correctamente las credenciales de MySQL en el archivo `.env`

#### 3. Frontend

```bash
# Navegar a la carpeta frontend
cd frontend

# Instalar dependencias
pnpm install

# Ejecutar el frontend
pnpm start
```

El frontend estará disponible en: http://localhost:4200

---

## ✨ Funcionalidades

1. **Autenticación JWT**: Login con usuario y contraseña, token de acceso con expiración
2. **CRUD Productos**: Crear, listar, editar y eliminar productos
3. **Sincronización Externa**: Importar productos desde https://fakestoreapi.com/products
4. **Movimientos de Stock**: Registrar entradas y salidas con actualización atómica del stock
5. **Búsqueda y Filtros**: Búsqueda por nombre, filtro por categoría
6. **Paginación del Servidor**: Paginación implementada con SQL (LIMIT/OFFSET)
7. **Validaciones**: Validaciones en backend (Bean Validation) y frontend (Reactive Forms)
8. **Manejo de Errores**: Exception Handler global con respuestas JSON descriptivas

---

## 🔐 Credenciales de Acceso

```
Usuario: admin
Password: admin123
```

---

## 📚 Preguntas de Conocimientos

### 1. Describa al menos dos formas de eliminar el problema N+1 en JPA

**Respuesta:**

El problema N+1 ocurre cuando se hace una consulta principal y luego N consultas adicionales para cargar relaciones, causando bajo rendimiento.

**Soluciones:**

- **Fetch Join (JPQL)**: Utilizar `JOIN FETCH` en consultas JPQL para cargar las entidades relacionadas en una sola consulta.
  ```java
  @Query("SELECT p FROM Producto p JOIN FETCH p.categoria WHERE p.id = :id")
  Producto findByIdWithCategoria(@Param("id") Long id);
  ```

- **Entity Graph**: Definir un `@EntityGraph` para especificar qué relaciones deben cargarse eagerly en una consulta específica.
  ```java
  @EntityGraph(attributePaths = {"categoria", "proveedor"})
  List<Producto> findAll();
  ```

- **Batch Fetching**: Configurar `@BatchSize` en las entidades relacionadas para cargarlas en lotes en lugar de una por una.
  ```java
  @OneToMany(mappedBy = "producto")
  @BatchSize(size = 10)
  private List<MovimientoStock> movimientos;
  ```

**Aplicación en este proyecto:**

Este proyecto **utiliza Spring Data JPA** (`ProductoRepository extends JpaRepository<Producto, Long>`). 

**Cómo se evitó N+1:**

1. **Diseño sin relaciones bidireccionales**: La entidad `Producto` NO tiene una colección `@OneToMany` hacia `MovimientoStock`, evitando lazy loading accidental que causaría N+1.

2. **Queries JPQL optimizadas**: En `ProductoRepository.findByFilters()` se usa una query JPQL personalizada con paginación del lado del servidor (`Pageable`), generando un solo SELECT con `LIMIT` y `OFFSET`.

3. **Queries separadas cuando se necesitan relaciones**: En `MovimientoStockRepository` se usa `@Query` con filtro por `productoId` para obtener movimientos específicos, evitando cargas automáticas.

4. **Si necesitara relaciones, se usaría `JOIN FETCH`**: El proyecto está preparado para agregar `JOIN FETCH` en caso de requerir carga eager de relaciones en una sola query.

Ver: `backend/src/main/java/com/inventario/repository/ProductoRepository.java`

---

### 2. En Angular Reactive Forms, defina un FormGroup con validaciones y mensajes de error

**Respuesta:**

Los Reactive Forms permiten un control programático de formularios con validaciones robustas y tipado fuerte.

**Ejemplo genérico:**

```typescript
export class ProductoFormComponent implements OnInit {
  productoForm: FormGroup;
  
  constructor(private fb: FormBuilder) {
    this.productoForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
      categoria: ['', Validators.required],
      proveedor: ['', [Validators.required, Validators.maxLength(100)]],
      precio: [0, [Validators.required, Validators.min(0.01)]],
      stock: [0, [Validators.required, Validators.min(0)]]
    });
  }
  
  getErrorMessage(field: string): string {
    const control = this.productoForm.get(field);
    if (!control || !control.errors || !control.touched) return '';
    
    if (control.errors['required']) return `${field} es requerido`;
    if (control.errors['minlength']) {
      return `${field} debe tener al menos ${control.errors['minlength'].requiredLength} caracteres`;
    }
    if (control.errors['min']) {
      return `${field} debe ser mayor a ${control.errors['min'].min}`;
    }
    return 'Campo inválido';
  }
}
```

**Aplicación en este proyecto:**

**Implementación en ProductoFormComponent:**

1. **FormGroup con validadores múltiples**: Se define un formulario reactivo con `FormBuilder` que incluye validadores para cada campo (`required`, `minLength`, `maxLength`, `min`, `max`).

2. **Método dinámico de errores**: `getErrorMessage(field)` inspecciona los errores del control y retorna mensajes descriptivos según el tipo de error detectado.

3. **Validación en template**: Se usa `*ngIf` para mostrar mensajes de error solo cuando el campo es `invalid` y ha sido `touched` por el usuario.

4. **Sincronización con backend**: Las validaciones del frontend replican las restricciones del backend (ej: nombre máx. 200 caracteres, precio > 0).

**Beneficios logrados:**
- ✅ Validación en tiempo real mientras el usuario escribe
- ✅ Mensajes de error descriptivos y dinámicos
- ✅ Prevención de envío si el formulario es inválido (`[disabled]="productoForm.invalid"`)
- ✅ Experiencia de usuario mejorada con feedback inmediato

Ver: `frontend/src/app/features/productos/producto-form.component.ts` y `.html`

---

### 3. Explique cómo configurar CORS y desactivar CSRF en un backend con JWT

**Respuesta:**

**CSRF (Cross-Site Request Forgery)**: Ataque donde un sitio malicioso hace peticiones usando la sesión del usuario.  
**CORS (Cross-Origin Resource Sharing)**: Mecanismo que permite a un navegador hacer peticiones a un dominio diferente.

**Configuración genérica en Spring Boot:**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Desactivar CSRF (no necesario en APIs REST stateless con JWT)
            .csrf(csrf -> csrf.disable())
            
            // Configurar CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configurar autorización
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/public/**").permitAll()
                .anyRequest().authenticated()
            )
            
            // Configurar sesiones stateless
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Agregar filtro JWT
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

**Aplicación en este proyecto:**

**Configuración en SecurityConfig:**

1. **CSRF desactivado**: Se usa `.csrf(AbstractHttpConfigurer::disable)` porque la API es stateless con JWT. El token viaja en el header `Authorization`, no en cookies, por lo que CSRF no aplica.

2. **CORS configurado desde .env**: Se lee `cors.allowed.origins` desde variables de entorno y se configura un `CorsConfigurationSource` que permite peticiones desde `http://localhost:4200` (frontend).

3. **Rutas públicas vs protegidas**: `/login`, `/error` y `/sync-products` son públicas; el resto requiere autenticación con `JwtAuthenticationFilter`.

4. **Sesiones stateless**: Se configura `SessionCreationPolicy.STATELESS` para que Spring no guarde sesiones en memoria.

**Frontend - Interceptor JWT:**

El `jwtInterceptor` (funcional) inyecta automáticamente el token en cada petición HTTP mediante el header `Authorization: Bearer <token>`, excepto en `/login`.

**¿Por qué es seguro?**
- JWT en headers evita ataques CSRF (no usa cookies automáticas)
- CORS restringe orígenes permitidos
- Cada petición es independiente (stateless)

Ver: `backend/src/main/java/com/inventario/config/SecurityConfig.java` y `frontend/src/app/core/interceptors/jwt.interceptor.ts`

---

### 4. Diseñe una transacción atómica para registrar movimientos de stock

**Respuesta:**

**Transacción atómica**: Garantiza que múltiples operaciones de base de datos se ejecuten como una unidad indivisible. Si alguna operación falla, todas se revierten (rollback).

**Configuración genérica:**

```java
@Service
public class MovimientoStockService {
    
    private final MovimientoStockRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    
    @Transactional
    public MovimientoStock registrarMovimiento(MovimientoStockRequest request) {
        // 1. Validar que el producto existe
        Producto producto = productoRepository.findById(request.getProductoId())
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        
        // 2. Calcular nuevo stock según el tipo de movimiento
        int cantidad = request.getTipo() == TipoMovimiento.ENTRADA 
            ? request.getCantidad() 
            : -request.getCantidad();
        
        int nuevoStock = producto.getStock() + cantidad;
        
        // 3. Validar que el stock no sea negativo
        if (nuevoStock < 0) {
            throw new IllegalArgumentException("Stock insuficiente para realizar la salida");
        }
        
        // 4. Registrar el movimiento (INSERT)
        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setProductoId(request.getProductoId());
        movimiento.setTipo(request.getTipo());
        movimiento.setCantidad(request.getCantidad());
        
        MovimientoStock movimientoGuardado = movimientoRepository.save(movimiento);
        
        // 5. Actualizar el stock del producto (UPDATE)
        producto.setStock(nuevoStock);
        productoRepository.save(producto);
        
        // Si ocurre algún error, @Transactional hace rollback automático
        return movimientoGuardado;
    }
}
```

**Aplicación en este proyecto:**

**Implementación en MovimientoStockService:**

1. **Anotación `@Transactional`**: Garantiza que las operaciones INSERT (en `movimientos_stock`) y UPDATE (en `productos`) ocurran atómicamente. Si alguna falla, Spring hace ROLLBACK automático.

2. **Validaciones previas**: Antes de las operaciones de BD, se valida:
   - Producto existe (`findById` con `orElseThrow`)
   - Stock suficiente para salidas
   - No hay overflow de enteros (`MAX_STOCK_VALUE`)

3. **Método `calcularNuevoStock()`**: Calcula el nuevo stock según tipo de movimiento (ENTRADA suma, SALIDA resta) con validaciones robustas.

4. **Logging detallado**: Se registra el stock anterior y nuevo para trazabilidad completa.

5. **Manejo de errores específico**: Excepciones descriptivas (`ResourceNotFoundException`, `IllegalArgumentException`) que activan el rollback automático.

**Flujo de la transacción:**
- BEGIN → SELECT producto → Validar → INSERT movimiento → UPDATE stock → COMMIT
- Si falla cualquier paso: ROLLBACK (no queda movimiento sin actualización de stock)

**Beneficios logrados:**
- ✅ Atomicidad: INSERT y UPDATE juntos o ninguno
- ✅ Consistencia: Validaciones previenen stock negativo/overflow
- ✅ Rollback automático ante errores
- ✅ Trazabilidad con logs estructurados

Ver: `backend/src/main/java/com/inventario/service/MovimientoStockService.java`

---

### 5. Enumere controles contra inyección SQL, XSS y manejo de roles de usuario

**Respuesta:**

**Aplicación en este proyecto:**

#### **A. Prevención de Inyección SQL:**

1. **Spring Data JPA con JPQL**: Uso de `@Query` con parámetros nombrados (`:search`, `:categoria`) que Hibernate convierte automáticamente en PreparedStatements seguros.
   - Ver: `ProductoRepository.findByFilters()` con `@Param`

2. **Bean Validation exhaustiva**: Todas las entidades (`Producto`) y DTOs (`ProductoRequest`, `MovimientoStockRequest`) usan anotaciones de validación:
   - `@NotBlank`, `@Size`, `@Min`, `@Max`, `@DecimalMin`, `@Digits`
   - Valida tipos de datos, longitudes y rangos antes de llegar a la BD

3. **`@Valid` en Controllers**: Cada endpoint valida automáticamente con `@Valid @RequestBody` antes de procesar.

#### **B. Prevención de XSS:**

1. **Angular escape automático**: Todas las interpolaciones `{{ }}` en templates son escapadas automáticamente por Angular.

2. **Sin `innerHTML` directo**: El proyecto no usa binding de HTML dinámico sin sanitizar.

3. **Validación en backend**: Los DTOs limitan caracteres permitidos y longitudes máximas para prevenir scripts maliciosos.

4. **Headers de seguridad**: Spring Security incluye headers por defecto (X-Content-Type-Options, X-Frame-Options).

#### **C. Manejo de Autenticación y Autorización:**

1. **JWT con roles embebidos**: `JwtUtil.generateToken()` incluye username y authorities en el token. El filtro `JwtAuthenticationFilter` valida el token en cada petición.

2. **AuthGuard en Frontend**: El `authGuard` funcional verifica si hay token válido antes de permitir acceso a rutas protegidas, redirigiendo a `/login` si no está autenticado.

3. **SecurityConfig con rutas diferenciadas**: `/login` es pública, todo lo demás requiere autenticación. Preparado para agregar roles específicos si se necesitan.

4. **Contraseñas encriptadas**: Se usa `BCryptPasswordEncoder` para hashear contraseñas en BD (ver `DataInitializer` y `CustomAuthenticationProvider`).

5. **Usuario en BD con version control**: La entidad `Usuario` incluye `@Version` para optimistic locking en actualizaciones concurrentes.

**Controles implementados:**
- ✅ Inyección SQL: JPA + JPQL + Bean Validation
- ✅ XSS: Angular escape automático + validaciones de entrada
- ✅ Autenticación: JWT con expiración
- ✅ Autorización: Guards en frontend + SecurityConfig en backend
- ✅ Contraseñas: BCrypt hashing

Ver: `SecurityConfig`, `JwtUtil`, `authGuard`, DTOs con `@Valid`, `CustomAuthenticationProvider`

---

## 📸 Demostración

Ver carpeta `/docs` para capturas de pantalla y video demostrativo del sistema en funcionamiento.

---

## 👨‍💻 Autor

**Johs Salinas**  
[GitHub](https://github.com/johssalinas)
