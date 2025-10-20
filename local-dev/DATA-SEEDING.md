# 🌱 Auto-Seeding Demo Data

## Overview

The application automatically seeds demo data on startup. This is a **professional pattern** used by real-world applications for initial setup and demos.

---

## ✅ What Gets Seeded

### 1. **Products** (Always)
- **23 The Office-themed products**
- Paper, office supplies, merchandise, party planning items
- Runs on every startup, but only if products table is empty
- **Idempotent** - safe to restart without duplicating data

### 2. **Demo Users** (Dev Profile Only)
- **4 pre-registered users** for testing
- Only seeds in `dev` profile (not in production)
- Passwords are BCrypt encrypted

---

## 👥 Demo Users (Dev Mode)

| Username | Password | Name | Email |
|----------|----------|------|-------|
| `mscott` | `worldsbestboss` | Michael Scott | michael.scott@dundermifflin.com |
| `dschrute` | `beetsfacts` | Dwight Schrute | dwight.schrute@dundermifflin.com |
| `jhalpert` | `tuna4life` | Jim Halpert | jim.halpert@dundermifflin.com |
| `pbeesly` | `fineart2023` | Pam Beesly | pam.beesly@dundermifflin.com |

**Quick Login:**
```json
{
    "username": "mscott",
    "password": "worldsbestboss"
}
```

---

## 📦 Sample Products

### Paper Products (Core Business)
- `DM-PAPER-001` - Dunder Mifflin Paper - Premium White ($6.99)
- `DM-PAPER-002` - Recycled Paper ($5.99)
- `DM-PAPER-003` - Colored Paper Pack ($8.99)
- `DM-PAPER-004` - Cardstock - Heavy Duty ($12.99)

### Office Supplies
- `DM-SUPPLY-001` - Stapler - Red Swingline ($15.99)
- `DM-SUPPLY-002` - Three-Hole Punch ($24.99)
- `DM-SUPPLY-003` - Binder Clips ($7.99)
- `DM-SUPPLY-004` - Sticky Notes ($4.99)

### Memorabilia
- `DM-MERCH-001` - World's Best Boss Mug ($12.99)
- `DM-MERCH-002` - Dundie Award Trophy ($19.99)
- `DM-MERCH-003` - Teapot - Ceramic ($24.99)
- `DM-MERCH-004` - Dwight Schrute Bobblehead ($29.99)

...and more!

---

## 🚀 How It Works

### On Application Startup:

```
1. ProductDataSeeder runs (Order: 1)
   ├── Check: Is products table empty?
   ├── If YES: Seed 23 products
   └── If NO: Skip seeding

2. DemoUserSeeder runs (Order: 2) [Dev Profile Only]
   ├── Check: Is users table empty?
   ├── If YES: Create 4 demo users
   └── If NO: Skip seeding
```

### Console Output:

```
🌱 Seeding initial product catalog (The Office Edition)...
✅ Product catalog seeded successfully! 23 products available.

👥 Seeding demo users (The Office Edition)...
✅ Demo users seeded! 4 users available for testing.
🔑 Login with: mscott/worldsbestboss or dschrute/beetsfacts
```

---

## 🎛️ Environment Profiles

### Development (Default)
```yaml
spring:
  profiles:
    active: dev
```
- ✅ Seeds products
- ✅ Seeds demo users
- Perfect for local development

### Production
```yaml
spring:
  profiles:
    active: prod
```
- ✅ Seeds products (catalog should exist)
- ❌ Skips demo users (security)
- Users must register via API

---

## 🔧 Implementation Details

### ProductDataSeeder
```java
@Component
@Order(1)  // Runs first
public class ProductDataSeeder implements CommandLineRunner {
    
    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            seedProducts();  // Only if empty
        }
    }
}
```

### DemoUserSeeder
```java
@Component
@Order(2)  // Runs second
@Profile("dev")  // Only in development
public class DemoUserSeeder implements CommandLineRunner {
    
    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            seedDemoUsers();  // Only if empty
        }
    }
}
```

---

## 🧪 Testing the Seeded Data

### 1. Start Fresh
```bash
# Drop and recreate database
cd local-dev
docker-compose down -v
docker-compose up -d

# Start application
cd ..
./mvnw spring-boot:run
```

You'll see seeding logs in the console!

### 2. Query Products
```bash
# Using psql
psql -h localhost -p 5433 -U order_user -d order_db -c "SELECT product_id, name, price FROM products LIMIT 5;"
```

### 3. Login with Demo User
```bash
# Using curl
curl -X POST http://localhost:8081/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"mscott","password":"worldsbestboss"}'
```

### 4. Create Order with Seeded Products
```bash
# Use JWT from login response
curl -X POST http://localhost:8081/api/orders \
  -H 'Authorization: Bearer YOUR_JWT_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "items": [
      {
        "productId": "DM-PAPER-001",
        "productName": "Dunder Mifflin Paper - Premium White",
        "quantity": 100,
        "price": 6.99
      }
    ]
  }'
```

---

## 🎯 Benefits

### For Development
- ✅ **Instant productivity** - No manual SQL setup
- ✅ **Consistent data** - Everyone has same products
- ✅ **Quick testing** - Login with pre-created users
- ✅ **Reproducible** - Fresh database = same data every time

### For Demos/Portfolio
- ✅ **Works out of the box** - Reviewers can start immediately
- ✅ **Professional** - Shows understanding of application initialization
- ✅ **Production pattern** - Real apps seed admin users, default configs
- ✅ **Idempotent** - Safe to restart without side effects

### For Production
- ✅ **Catalog ready** - Products exist on first deploy
- ✅ **No demo users** - Security maintained
- ✅ **Controlled seeding** - Only runs when tables empty

---

## 🔄 Updating Seed Data

### Add New Products
Edit `ProductDataSeeder.java`:

```java
createProduct(
    "DM-NEW-001",
    "New Product Name",
    new BigDecimal("29.99"),
    "Product description",
    100  // stock quantity
)
```

### Disable Demo Users
Set profile to `prod` in `application.yml`:

```yaml
spring:
  profiles:
    active: prod
```

Or run with:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

---

## 📊 Database Schema

The seeder creates data that matches your entities:

**Products:**
```sql
product_id | name | price | description | stock_quantity | created_at
```

**Users (Dev Only):**
```sql
id | username | email | password (BCrypt) | first_name | last_name | created_at
```

---

## 🎬 Real-World Examples

This pattern is used by:
- **Shopify** - Seeds sample products in dev stores
- **Auth0** - Seeds default admin user
- **Keycloak** - Seeds default realm
- **Spring Security Samples** - Seeds demo users

**Your implementation follows industry best practices!** 🏆

---

## 🚨 Important Notes

1. **Idempotent** - Safe to restart, won't duplicate data
2. **Profile-aware** - Different behavior in dev vs prod
3. **Order matters** - Products seed before users (dependencies)
4. **Password security** - Demo passwords are BCrypt encrypted
5. **Logged clearly** - Easy to see what's happening on startup

---

## ✅ Summary

**You now have:**
- ✅ 23 themed products auto-seeded
- ✅ 4 demo users (dev mode) ready to login
- ✅ Professional data initialization
- ✅ Production-ready seeding strategy
- ✅ Zero manual SQL required!

**Next time you start the app:**
- Products will be there
- Demo users will be there (dev mode)
- You can login and create orders immediately

**Ship it!** 🚀

