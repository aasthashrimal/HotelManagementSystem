# 🏨 Hotel Grand – Management System
### College Project | Java + JavaFX + SQLite

---

## 📋 PREREQUISITES

| Tool | Minimum Version | Download |
|------|----------------|---------|
| JDK  | 17 or above    | https://adoptium.net |
| Maven | 3.6+          | https://maven.apache.org |
| JavaFX SDK | 21+ (you have 25.0.1 ✓) | Already installed |

---

## ⚙️ SETUP STEPS (Windows)

### Step 1 – Verify Java
Open Command Prompt and run:
```
java -version
```
Should show `17` or higher.

---

### Step 2 – Verify Maven
```
mvn -version
```
If not installed:
1. Download from https://maven.apache.org/download.cgi
2. Extract to `C:\maven`
3. Add `C:\maven\bin` to your PATH environment variable

---

### Step 3 – Update JavaFX Path in pom.xml (if needed)
The `pom.xml` uses Maven to download JavaFX automatically from Maven Central.
**Your installed JavaFX SDK is only needed to RUN the jar.**

Open `run.bat` and check this line:
```
set JAVAFX_PATH=C:\Program Files\Java\javafx-sdk-25.0.1\lib
```
This is already set to your path ✓

---

### Step 4 – Build the Project
Open Command Prompt **in the project folder** and run:
```
mvn clean package
```
Wait for `BUILD SUCCESS`.

---

### Step 5 – Run the Application

**Option A – Use the batch file:**
```
run.bat
```

**Option B – Run manually:**
```
java --module-path "C:\Program Files\Java\javafx-sdk-25.0.1\lib" ^
     --add-modules javafx.controls,javafx.fxml ^
     -jar target\HotelManagementSystem-1.0-SNAPSHOT.jar
```

---

## 🔐 LOGIN CREDENTIALS

| Username | Password | Role |
|----------|----------|------|
| admin    | admin123 | Administrator |
| staff    | staff123 | Staff |

---

## 📁 PROJECT STRUCTURE

```
HotelManagementSystem/
├── src/main/java/com/hotel/
│   ├── model/          ← OOP models (Room, Customer, Booking, Staff)
│   ├── dao/            ← Database layer (JDBC + SQLite)
│   ├── service/        ← Business logic (Booking, Invoice, Threads)
│   ├── controller/     ← JavaFX controllers (9 screens)
│   ├── util/           ← FileLogger, Serialization, Email, Validation
│   └── main/           ← MainApp.java (entry point)
├── src/main/resources/
│   ├── com/hotel/view/ ← FXML files for all screens
│   └── css/style.css   ← Dark blue theme stylesheet
├── pom.xml             ← Maven config + dependencies
├── run.bat             ← Windows run script
└── hotel.db            ← SQLite database (auto-created on first run)
```

Generated folders at runtime:
```
logs/       ← booking.log, error.log
invoices/   ← invoice_<id>.txt files
reports/    ← CSV exports
backup/     ← Serialized backups (.ser files)
```

---

## 📚 LAB CONCEPTS COVERED

| Week | Concept | Where Used |
|------|---------|-----------|
| Week 1 | OOP (Classes, Encapsulation, Inheritance, Polymorphism, Abstraction, Interface) | `model/` package - Room hierarchy |
| Week 2 | Wrapper Classes, Enum, Autoboxing | `RoomType.java`, `calculateTariff()` methods |
| Week 3 | Multithreading (Thread, sleep, join, yield) | `BookingService`, `RoomServiceTask`, `EmailUtil` |
| Week 4 | Synchronization (synchronized, wait, notify) | `BookingService.bookRoom()`, `RoomServiceTask` |
| Week 5 | File I/O (FileWriter, BufferedReader, CSV) | `FileLogger.java`, invoice saving, CSV export |
| Week 6 | Serialization (ObjectOutputStream/InputStream) | `SerializationUtil.java`, backup/restore |
| Week 7 | Generics (Generic class, methods, bounded) | `Pair<A,B>` in `util/Pair.java` |
| Week 8 | Collections (ArrayList, HashMap, Iterator, Sort) | `RoomService`, `BookingDAO`, `InvoiceService` |
| Week 9 | JavaFX (TableView, ComboBox, FXML, Events) | All `controller/` + `view/` files |

---

## 🏗️ FEATURES

- ✅ Login System (Admin/Staff)
- ✅ Dashboard with live stats
- ✅ Room Management (CRUD + filter + sort)
- ✅ Customer Management (CRUD + search + validation)
- ✅ Booking System (book / checkout / cancel / double-booking prevention)
- ✅ Invoice Generation (with GST, saved to file)
- ✅ Staff Management (add/update/delete + task assignment)
- ✅ Services Module (food, cleaning, maintenance)
- ✅ Reports & Analytics (revenue, occupancy rate)
- ✅ File Logging (booking logs + error logs)
- ✅ CSV Export
- ✅ Serialization Backup & Restore
- ✅ Dark Mode UI

---

## 📧 EMAIL SETUP (Optional)

Edit `src/main/java/com/hotel/util/EmailUtil.java`:
```java
private static final String FROM_EMAIL = "your_email@gmail.com";
private static final String APP_PASSWORD = "your_gmail_app_password";
```

To get Gmail App Password:
1. Go to Google Account → Security → 2-Step Verification (enable it)
2. Go to App Passwords → Generate for "Mail" + "Windows Computer"
3. Use that 16-character password above

---

## 🐛 TROUBLESHOOTING

**"Error: JavaFX runtime components are missing"**
→ Make sure you're using `run.bat` or the full `java --module-path` command

**"BUILD FAILURE" in Maven**
→ Check internet connection (Maven downloads dependencies on first run)
→ Run `mvn clean package` again

**"Database locked"**
→ Close any other running instance of the app

**Table shows no data**
→ The database `hotel.db` is auto-created in the project folder with sample data
→ Delete `hotel.db` to reset everything
