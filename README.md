## Order Service Unit Tests (OrderServiceImpl & OrderServiceImplTest)

### Tổng quan
Tài liệu này mô tả unit test cho `OrderServiceImpl` (business logic xử lý giao dịch/đơn hàng) được hiện thực trong `OrderServiceImplTest`. Các test sử dụng Mockito để mock toàn bộ phụ thuộc (repository/service) nhằm chạy nhanh, ổn định, không cần kết nối CSDL và không load Spring context.

### Cấu trúc dự án liên quan

```
Web-KopiCafeBE/
  pom.xml
  src/
    main/
      java/com/kopi/kopi/service/impl/OrderServiceImpl.java
      java/com/kopi/kopi/controller/GuestOrderController.java
      java/com/kopi/kopi/service/TableService.java
      java/com/kopi/kopi/repository/*.java
    test/
      java/com/kopi/kopi/service/impl/OrderServiceImplTest.java
  target/site/jacoco/
    index.html
    com.kopi.kopi.service.impl/OrderServiceImpl.java.html
```

### Yêu cầu hệ thống
- Java Development Kit: JDK 22 (theo `pom.xml` `<java.version>22</java.version>`)
- Maven 3.9+ (khuyến nghị)
- Kết nối Internet để tải dependency Maven lần đầu

### Cách chạy test (Terminal)
- Chạy chỉ unit test cho OrderService (tránh fail do Spring Boot context/DB của các test khác):
  - PowerShell (Windows):
    1) `cd "d:\Code\dev files\GitHub\KopiCoffee\Web-KopiCafeBE"`
    2) `mvn -q -Dtest=OrderServiceImplTest test`
  - Bash (macOS/Linux):
    1) `cd "d:/Code/dev files/GitHub/KopiCoffee/Web-KopiCafeBE"`
    2) `mvn -q -Dtest=OrderServiceImplTest test`

- Sinh báo cáo coverage (JaCoCo) cho test trên:
  - `mvn -q -Dtest=OrderServiceImplTest test jacoco:report`
  - Mở: `target/site/jacoco/index.html` hoặc trực tiếp `target/site/jacoco/com.kopi.kopi.service.impl/OrderServiceImpl.java.html`

### Coverage Metrics

- **Instruction Coverage**: 93% (1,251/1,336 instructions)

- **Branch Coverage**: 64% (94/146 branches)

- **Line Coverage**: 95% (250/263 lines)

- **Method Coverage**: 100% (8/8 methods)

- **Class Coverage**: 100% (1/1 class)

### Coverage by Class

- **OrderServiceImpl**: 93% instruction coverage (85/1,336 instructions missed) ✅

Lưu ý: Chạy toàn bộ test (`mvn test`) có thể thất bại do `KopiApplicationTests` tải Spring context và cố kết nối SQL Server thật. Để tránh, chỉ định rõ lớp test cần chạy như trên.

### Dependencies test & toolchain
- JUnit Jupiter 5.10.2 (`org.junit.jupiter:junit-jupiter`)
- Spring Boot Starter Test (`org.springframework.boot:spring-boot-starter-test`)
- Mockito JUnit Jupiter 5.12.0 (`org.mockito:mockito-junit-jupiter`)
- AssertJ 3.26.0 (`org.assertj:assertj-core`)
- JaCoCo Maven Plugin 0.8.12 (coverage report)

Ghi chú: JVM có thể log cảnh báo ByteBuddy agent khi dùng Mockito inline; có thể bỏ qua hoặc cấu hình theo tài liệu Mockito nếu muốn tắt cảnh báo.

### Phạm vi test case trong OrderServiceImplTest
- listPending(status, type=null, page, limit)
  - Trả về trang đầu đúng meta
  - Trả về trang cuối đúng meta
  - Map các sản phẩm (name/qty/subtotal)

- getUserTransactions(userId, page, limit)
  - Một đơn hàng có sản phẩm, validate grand_total/status_name/products/meta
  - totalAmount null => grand_total = 0
  - Status CANCELLED
  - Pagination data lớn (kiểm tra meta, kích thước data)

- getTransactionDetail(id, userId)
  - Chủ sở hữu: 200 OK, validate receiver_name, delivery_address, payment_name, products
  - Không phải chủ sở hữu: 403 Forbidden

- changeStatus(id, payload)
  - Status invalid => 400 Bad Request
  - COMPLETED: cập nhật status đơn và payment => PAID, lưu đơn, callback giải phóng bàn nếu có bàn

- createTransaction(body, currentUser)
  - Khách hàng (role=3): có address, payment pending
  - Nhân viên (role=2): có customer_id, payment banking + paid=true

- createGuestTableOrder(request)
  - Thiếu token & table_number => 400
  - Không tìm thấy bàn => 404
  - Bàn DISABLED => 400
  - Không có sản phẩm => 400
  - Tạo thành công: trả về id/table_number/status, setOccupiedIfHasPendingOrders

### Báo cáo coverage (trích từ JaCoCo)

Kết quả dưới đây được sinh khi chỉ chạy `OrderServiceImplTest`.

- Coverage của `OrderServiceImpl`:
  - Instruction: ~93.6% (1251/1336)
  - Branch: ~64.4% (94/146)
  - Line: ~95.1% (250/263)
  - Complexity: ~42.0% (34/81)
  - Method: 100% (8/8)

- Coverage by class (gói `com.kopi.kopi.service.impl`, rút gọn):
  - OrderServiceImpl: Instruction ~93.6%, Line ~95.1%, Branch ~64.4%
  - UserServiceImpl, TableServiceImpl, AuthServiceImpl, PromoServiceImpl, EmailServiceImpl, SmtpEmailService: thấp (gần 0–20%) do không có test tương ứng trong lần chạy này

Xem chi tiết từng dòng/nhánh: `target/site/jacoco/com.kopi.kopi.service.impl/OrderServiceImpl.java.html`.

### Giới hạn & rủi ro
- Test hiện là unit test sử dụng Mockito, không kiểm tra tích hợp DB/transaction thật, security filter, hay cấu hình Spring.
- Nhánh `listPending` với `type="TABLE"`/`"SHIPPING"` chưa có test riêng (mặc dù nhánh mặc định đã cover). Có thể bổ sung để tăng branch coverage.
- Các service khác (Auth/Promo/User/Table/Email) chưa được bao phủ trong lần chạy này => tổng coverage toàn module sẽ thấp nếu chạy chung.
- Chạy toàn bộ test sẽ cố gắng kết nối SQL Server thực (do `KopiApplicationTests`), có thể fail nếu không có DB. Nên tách unit/integration test profile hoặc chỉ định lớp test cần chạy.

### Mẹo mở rộng coverage nhanh
- Thêm test cho `listPending` với `type="TABLE"` và `type="SHIPPING"` (mock các phương thức repository tương ứng).
- Thêm test cho `changeStatus` với `CANCELLED` và `PENDING` để cover cập nhật `PaymentStatus` các nhánh còn lại.
- Bổ sung test cho trường hợp `getTransactionDetail` không có `payments` hoặc `productNameSnapshot=null` để cover các fallback.

### Liên quan
- Service: `src/main/java/com/kopi/kopi/service/impl/OrderServiceImpl.java`
- Test: `src/test/java/com/kopi/kopi/service/impl/OrderServiceImplTest.java`
- Coverage: `target/site/jacoco/`

### Slides
https://www.canva.com/design/DAG2hyCihKA/XGE_-GxJer5G4_NTOs-Itg/edit?utm_content=DAG2hyCihKA&utm_campaign=designshare&utm_medium=link2&utm_source=sharebutton

