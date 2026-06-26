=== SECTION 1: Setup (prerequisite data) ===

Step-by-step: what data must exist before testing

1. Giả định có ít nhất 2 nhân viên ở vai trò STAFF (ví dụ: Staff A, Staff B) có trạng thái hoạt động (active = true) và 1 nhân viên STAFF ở trạng thái khóa (Staff C, active = false).
2. Có ít nhất 1 tài khoản vai trò MANAGER hoặc ADMIN để thực hiện các thao tác quản lý.
3. Có ít nhất 2 mẫu ca làm việc (Shift Templates) được tạo sẵn và đang hoạt động (active = true), ví dụ: Ca Sáng (minStaff = 2, 08:00 - 12:00) và Ca Chiều (minStaff = 1, 13:00 - 17:00).
4. Có ít nhất 1 mẫu ca làm việc ở trạng thái không hoạt động (active = false) hoặc đã hết hạn hiệu lực để kiểm tra loại trừ.
5. Tạo sẵn các ca làm việc ở trạng thái nháp (DRAFT) khớp với các mẫu ca trên cho tuần tới (ví dụ: Thứ Hai tuần tới có Ca Sáng ở trạng thái DRAFT).

=== SECTION 2: Staff — Khai báo lịch rảnh ===

6. Trường hợp kiểm thử 1: Nhân viên đăng ký lịch rảnh thành công cho ca đang hoạt động
   - Precondition: Nhân viên A đã đăng nhập thành công vào hệ thống.
   - Action: Nhân viên A chọn mẫu Ca Sáng vào ngày Thứ Hai và chọn có thể làm việc (isAvailable = true).
   - Expected result: Hệ thống trả về mã thành công 200 OK.
   - What to verify in DB or UI: Trong bảng employee_weekly_availabilities xuất hiện bản ghi tương ứng có isAvailable = true. Trên giao diện của nhân viên, nút gạt ca làm việc hiển thị trạng thái đã bật.

7. Trường hợp kiểm thử 2: Nhân viên cập nhật lịch rảnh thành công từ rảnh sang bận
   - Precondition: Nhân viên A có sẵn lịch rảnh cho Ca Sáng Thứ Hai (isAvailable = true).
   - Action: Nhân viên A chuyển trạng thái thành bận (isAvailable = false) cho ca đó.
   - Expected result: Hệ thống trả về mã thành công 200 OK.
   - What to verify in DB or UI: Bản ghi trong cơ sở dữ liệu được cập nhật trường isAvailable = false và updatedAt được cập nhật thời gian hiện tại.

=== SECTION 3: Manager — Chạy preview ===

8. Trường hợp kiểm thử 3: Chạy xem trước lịch ca thành công với đầy đủ nhân sự (Happy Path)
   - Precondition: Quản lý đã đăng nhập. Có 1 ca nháp Ca Sáng vào Thứ Hai tuần tới (yêu cầu tối thiểu 2 người). Nhân viên A và Nhân viên B đều đăng ký rảnh cho Ca Sáng vào Thứ Hai.
   - Action: Quản lý gọi API POST /api/v1/scheduling/preview với tham số ngày bắt đầu và kết thúc bao gồm ngày Thứ Hai đó.
   - Expected result: Trả về danh sách xem trước chứa thông tin Ca Sáng của ngày Thứ Hai với danh sách nhân viên gợi ý gồm Nhân viên A và Nhân viên B. Trường isFulfilled là true và missingCount là 0.
   - What to verify in DB or UI: Trên giao diện Android, danh sách xem trước hiển thị Ca Sáng kèm tên Nhân viên A, Nhân viên B và có huy hiệu màu xanh ghi Đủ người.

9. Trường hợp kiểm thử 4: Chạy xem trước lịch ca khi không có ca nháp nào trong khoảng thời gian chọn
   - Precondition: Không có ca làm việc nào ở trạng thái DRAFT trong khoảng ngày được chọn.
   - Action: Quản lý chạy xem trước trong khoảng ngày này.
   - Expected result: Trả về danh sách rỗng với mã 200 OK.
   - What to verify in DB or UI: Trên giao diện Android hiển thị trạng thái màn hình rỗng với thông báo không có ca nào cần xếp lịch.

10. Trường hợp kiểm thử 5: Loại trừ nhân viên có đơn xin nghỉ đã được duyệt
    - Precondition: Nhân viên B có đơn xin nghỉ ở trạng thái APPROVED trùng với thời gian diễn ra Ca Sáng Thứ Hai.
    - Action: Quản lý chạy xem trước lịch ca cho ngày Thứ Hai.
    - Expected result: Nhân viên B bị loại khỏi danh sách gợi ý. Danh sách gợi ý chỉ chứa Nhân viên A. Trường isFulfilled là false và missingCount là 1.
    - What to verify in DB or UI: Trên giao diện hiển thị Ca Sáng kèm huy hiệu cảnh báo màu cam Thiếu 1 người và danh sách chỉ có Nhân viên A.

11. Trường hợp kiểm thử 6: Loại trừ nhân viên đã được phân công ca trùng thời gian trên cùng ngày
    - Precondition: Nhân viên A đã được phân công vào một ca làm việc khác trùng hoặc chồng lấn thời gian với Ca Sáng Thứ Hai.
    - Action: Quản lý chạy xem trước lịch ca.
    - Expected result: Nhân viên A bị loại khỏi danh sách gợi ý của Ca Sáng Thứ Hai để tránh trùng lịch.
    - What to verify in DB or UI: Gợi ý của Ca Sáng Thứ Hai không chứa Nhân viên A trong phần suggestedUserIds.

12. Trường hợp kiểm thử 7: Số lượng nhân viên rảnh không đạt chỉ tiêu tối thiểu của ca
    - Precondition: Ca Sáng yêu cầu tối thiểu 2 người, nhưng chỉ có duy nhất Nhân viên A đăng ký rảnh cho ca này.
    - Action: Quản lý chạy xem trước lịch ca.
    - Expected result: Trả về gợi ý chứa Nhân viên A, trường isFulfilled là false và missingCount là 1.
    - What to verify in DB or UI: Giao diện Android hiển thị cảnh báo thiếu người rõ ràng cho ca này.

=== SECTION 4: Manager — Apply preview ===

13. Trường hợp kiểm thử 8: Áp dụng lịch xem trước thành công để tạo phân công ca làm việc
    - Precondition: Quản lý đã chạy xem trước thành công và nhận được mã runId từ hệ thống.
    - Action: Quản lý gọi API POST /api/v1/scheduling/preview/runId/apply với runId hợp lệ.
    - Expected result: Hệ thống trả về mã thành công 200 OK cùng thông tin lịch ca đã áp dụng. Các ca nháp được phân công nhân sự tương ứng.
    - What to verify in DB or UI: Trong bảng shift_assignments xuất hiện các dòng phân công nhân sự cho Nhân viên A và Nhân viên B vào Ca Sáng Thứ Hai. Trên giao diện danh sách ca làm việc của quản lý hiển thị các ca này đã có nhân viên được gán vào.

14. Trường hợp kiểm thử 9: Chặn áp dụng lịch ca hai lần cho cùng một mã runId
    - Precondition: Quản lý đã áp dụng thành công mã runId một lần.
    - Action: Quản lý gửi lại yêu cầu áp dụng cho chính mã runId đó lần thứ hai.
    - Expected result: Hệ thống chặn và trả về lỗi HTTP 400 Bad Request hoặc thông báo ca làm việc đã được áp dụng trước đó.
    - What to verify in DB or UI: Số lượng bản ghi phân công trong bảng shift_assignments không bị nhân đôi hoặc thay đổi.

15. Trường hợp kiểm thử 10: Áp dụng mã lịch ca không tồn tại hoặc đã hết hiệu lực
    - Action: Quản lý gọi API apply với một mã runId ngẫu nhiên không tồn tại.
    - Expected result: Hệ thống trả về lỗi HTTP 404 Not Found.

=== SECTION 5: Conflict check (Section 16 rule) ===

16. Trường hợp kiểm thử 11: Nhân viên hủy lịch rảnh khi chưa được phân công vào ca tương lai nào
    - Precondition: Nhân viên A đang đăng ký rảnh cho Ca Sáng Thứ Hai, nhưng không được gán vào bất kỳ ca tương lai nào của mẫu ca này.
    - Action: Nhân viên A cập nhật trạng thái lịch rảnh thành bận (isAvailable = false).
    - Expected result: Yêu cầu được chấp nhận, hệ thống trả về mã 200 OK.
    - What to verify in DB or UI: Lịch rảnh trong DB được đổi thành false thành công.

17. Trường hợp kiểm thử 12: Nhân viên bị chặn hủy lịch rảnh khi đã được phân công vào ca đã công bố (PUBLISHED)
    - Precondition: Nhân viên A đã được phân công vào Ca Sáng Thứ Hai tuần tới và ca này đã được công bố ở trạng thái PUBLISHED hoặc OPEN.
    - Action: Nhân viên A cố gắng cập nhật trạng thái lịch rảnh của Ca Sáng Thứ Hai thành bận (isAvailable = false).
    - Expected result: Hệ thống trả về lỗi xung đột HTTP 409 Conflict kèm theo danh sách các ca làm việc bị ảnh hưởng.
    - What to verify in DB or UI: Trên giao diện ứng dụng của nhân viên hiển thị thông báo lỗi chi tiết Bạn đang được xếp vào các ca sau kèm danh sách các ca cụ thể. Trạng thái trong DB không thay đổi.

18. Trường hợp kiểm thử 13: Nhân viên hủy lịch rảnh khi chỉ được phân công vào ca nháp (DRAFT)
    - Precondition: Nhân viên A được phân công vào Ca Sáng Thứ Hai tuần tới nhưng ca này vẫn ở trạng thái nháp DRAFT.
    - Action: Nhân viên A cập nhật trạng thái lịch rảnh thành bận (isAvailable = false).
    - Expected result: Yêu cầu được chấp nhận, trả về mã 200 OK. Nhân viên được cảnh báo hoặc cho phép cập nhật bình thường vì ca chưa công bố.
    - What to verify in DB or UI: Trạng thái cập nhật thành công trong DB.

19. Trường hợp kiểm thử 14: Nhân viên hủy lịch rảnh khi ca trùng đã đóng (CLOSED)
    - Precondition: Nhân viên A có ca làm việc trùng trong quá khứ đã hoàn thành ở trạng thái CLOSED hoặc CANCELLED.
    - Action: Nhân viên A cập nhật trạng thái lịch rảnh thành bận (isAvailable = false).
    - Expected result: Yêu cầu được chấp nhận thành công với mã 200 OK vì ca đã kết thúc hoặc đã hủy không bị ảnh hưởng.

=== SECTION 6: Leave Request flow ===

20. Trường hợp kiểm thử 15: Quy trình gửi đơn xin nghỉ của nhân viên
    - Precondition: Nhân viên điền đầy đủ lý do nghỉ, thời gian bắt đầu và kết thúc hợp lệ.
    - Action: Nhân viên bấm gửi đơn xin nghỉ.
    - Expected result: Đơn xin nghỉ được tạo thành công ở trạng thái PENDING.

21. Trường hợp kiểm thử 16: Quản lý duyệt đơn và tự động gỡ phân công các ca nháp và ca đã công bố
    - Precondition: Nhân viên A có đơn xin nghỉ đang ở trạng thái PENDING. Nhân viên A đang được gán vào 3 ca trùng thời gian nghỉ: Ca 1 (DRAFT), Ca 2 (PUBLISHED) và Ca 3 (IN_PROGRESS hoặc CLOSED).
    - Action: Quản lý phê duyệt đơn xin nghỉ của Nhân viên A.
    - Expected result: Đơn xin nghỉ chuyển sang trạng thái APPROVED. Phân công của Nhân viên A tại Ca 1 và Ca 2 bị xóa tự động. Phân công tại Ca 3 được giữ nguyên. Trường affectedAssignmentCount trả về giá trị là 2.
    - What to verify in DB or UI: Trong bảng shift_assignments, các bản ghi của Nhân viên A tại Ca 1 và Ca 2 bị xóa. Trên giao diện duyệt đơn của quản lý hiển thị số ca bị ảnh hưởng là 2.

22. Trường hợp kiểm thử 17: Quản lý từ chối đơn xin nghỉ và giữ nguyên phân công ca
    - Precondition: Nhân viên A có đơn xin nghỉ PENDING và các ca phân công trùng lịch.
    - Action: Quản lý từ chối đơn xin nghỉ.
    - Expected result: Đơn xin nghỉ chuyển sang trạng thái REJECTED. Tất cả các phân công ca trùng lịch của Nhân viên A được giữ nguyên. Trường affectedAssignmentCount trả về là 0.
    - What to verify in DB or UI: Kiểm tra bảng shift_assignments không có bản ghi nào bị xóa.

23. Trường hợp kiểm thử 18: Phê duyệt đơn xin nghỉ trùng với nhiều ca làm việc đủ điều kiện
    - Precondition: Nhân viên A đăng ký nghỉ dài ngày trùng với 5 ca làm việc ở trạng thái PUBLISHED và DRAFT.
    - Action: Quản lý phê duyệt đơn nghỉ.
    - Expected result: Cả 5 phân công ca làm việc trùng lịch đều bị xóa tự động khỏi bảng shift_assignments.

=== SECTION 7: UI flow (manual) ===

Quy trình bấm nút của người kiểm thử trên ứng dụng Android:

24. Bước 1: Người kiểm thử đăng nhập bằng tài khoản Quản lý hoặc Admin.
25. Bước 2: Từ màn hình chính, truy cập vào chức năng Lịch ca làm việc (màn hình ShiftScheduleActivity).
26. Bước 3: Nhấp vào nút dấu cộng (+) ở góc trên cùng bên phải màn hình.
27. Bước 4: Trên hộp thoại lựa chọn vừa xuất hiện, nhấp chọn mục Tự động sắp xếp ca để mở màn hình SchedulingPreviewActivity.
28. Bước 5: Trên màn hình xem trước, nhấp chọn nút Từ ngày và chọn ngày Thứ Hai tuần tới từ lịch hiển thị.
29. Bước 6: Nhấp chọn nút Đến ngày và chọn ngày Thứ Ba tuần tới. Sau khi chọn xong, nút Chạy phân lịch sẽ chuyển sang trạng thái hoạt động (enabled).
30. Bước 7: Nhấp vào nút Chạy phân lịch. Quan sát thấy thanh tiến trình chạy và biến mất, sau đó danh sách các ca làm việc gợi ý hiển thị trên màn hình.
31. Bước 8: Kiểm tra thông tin các ca gợi ý, xem tên nhân sự có đúng với khai báo lịch rảnh không, các ca đủ người hiển thị badge màu xanh lá và ca thiếu người hiển thị badge màu cam.
32. Bước 9: Kiểm tra dòng tổng kết ở cuối màn hình hiển thị chính xác tổng số ca, số ca đủ và số ca thiếu.
33. Bước 10: Nhấp vào nút Áp dụng tất cả ở dưới cùng. Sau khi hoàn tất, hệ thống hiển thị thông báo Toast thành công và tự động đóng màn hình xem trước để quay lại màn hình Lịch ca làm việc.
34. Bước 11: Kiểm tra tại màn hình Lịch ca làm việc xem các nhân sự gợi ý đã được tự động điền vào danh sách phân công của các ca tương ứng hay chưa.

=== SECTION 8: Edge cases ===

35. Trường hợp kiểm thử 19: Ngày kết thúc nhỏ hơn ngày bắt đầu
    - Action: Người kiểm thử chọn Từ ngày là ngày mai và Đến ngày là ngày hôm nay.
    - Expected result: Ứng dụng Android chặn không cho chạy bằng cách giữ nút Chạy phân lịch ở trạng thái vô hiệu hóa (disabled).
36. Trường hợp kiểm thử 20: Hệ thống không có bất kỳ dữ liệu lịch rảnh nào từ nhân viên
    - Action: Quản lý chạy xem trước lịch ca khi không nhân viên nào khai báo lịch rảnh.
    - Expected result: Tất cả các ca làm việc gợi ý trả về đều hiển thị danh sách nhân viên rỗng, trường isFulfilled là false và missingCount bằng đúng minStaff của ca.
37. Trường hợp kiểm thử 21: Ca làm việc có mẫu ca đã bị vô hiệu hóa
    - Precondition: Ca làm việc được tạo từ một mẫu ca nhưng hiện tại mẫu ca đó đã bị chuyển sang trạng thái không hoạt động (active = false).
    - Action: Quản lý chạy xem trước lịch ca.
    - Expected result: Ca làm việc đó sẽ bị bỏ qua và không xuất hiện trong danh sách gợi ý xem trước.
38. Trường hợp kiểm thử 22: Gọi API apply với mã xem trước không tồn tại
    - Action: Gọi POST trực tiếp tới đường dẫn apply với mã runId sai lệch.
    - Expected result: Trả về mã lỗi HTTP 404 Not Found.
39. Trường hợp kiểm thử 23: Tài khoản không có quyền quản lý cố gắng chạy phân lịch
    - Precondition: Tài khoản đăng nhập có vai trò STAFF.
    - Action: Gọi POST trực tiếp tới đường dẫn preview hoặc apply của lịch ca tự động.
    - Expected result: Trả về mã lỗi HTTP 403 Forbidden.
