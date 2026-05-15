# 📱 Đặc Tả Chi Tiết 9 Màn Hình UI — Ứng Dụng Đọc Sách & Sách Nói (Android Java)

> **Dự án:** Ứng dụng Sách Nói & Ebook (tham khảo Fonos, Voiz FM)  
> **Nền tảng:** Android — Java + XML  
> **Kiến trúc:** Single-Activity + Jetpack Navigation Component + Fragment  
> **Timeline:** 7 ngày (tuần 1 — UI only, chưa kết nối backend)

---

## Mục Lục

1. [Màn Hình Đăng Nhập (Login)](#1-màn-hình-đăng-nhập-login)
2. [Màn Hình Đăng Ký (Sign Up)](#2-màn-hình-đăng-ký-sign-up)
3. [Trang Chủ (Books Hub)](#3-trang-chủ-books-hub)
4. [Khám Phá (Discovery)](#4-khám-phá-discovery)
5. [Thư Viện (Library)](#5-thư-viện-library)
6. [Chi Tiết Sách (Book Detail)](#6-chi-tiết-sách-book-detail)
7. [Trình Phát Audio (Audio Player)](#7-trình-phát-audio-audio-player)
8. [Trình Đọc Ebook (Ebook Reader)](#8-trình-đọc-ebook-ebook-reader)
9. [Hồ Sơ Cá Nhân (Profile)](#9-hồ-sơ-cá-nhân-profile)

---

## 1. Màn Hình Đăng Nhập (Login)

### Mô Tả Chức Năng
Màn hình xác thực danh tính người dùng. Không sử dụng full-screen truyền thống mà hiển thị dưới dạng **BottomSheetDialogFragment** kéo lên từ dưới, phủ mờ nội dung phía sau. Cách này tạo cảm giác liền mạch và cho phép người dùng đã lướt xem nội dung miễn phí trước khi được yêu cầu đăng nhập để tiếp cận tính năng sâu hơn.

###  Giao Diện (Layout XML)

**File:** `fragment_login_bottom_sheet.xml`  
**Component:** `BottomSheetDialogFragment`

```
┌─────────────────────────────────┐
│  ────  (drag handle indicator)   │
│                                  │
│     Tên ứng dụng / Logo        │
│   "Đăng nhập để tiếp tục"        │
│                                  │
│  ┌──────────────────────────┐    │
│  │   Tiếp tục với Google  │    │
│  └──────────────────────────┘    │
│  ┌──────────────────────────┐    │
│  │   Tiếp tục với Facebook│    │
│  └──────────────────────────┘    │
│  ┌──────────────────────────┐    │
│  │   Tiếp tục với Apple   │    │
│  └──────────────────────────┘    │
│                                  │
│         ─── hoặc ───             │
│                                  │
│  ┌──────────────────────────┐    │
│  │   Email hoặc SĐT       │    │
│  └──────────────────────────┘    │
│                                  │
│  Chưa có tài khoản? [Đăng ký]   │
│  [Điều khoản] · [Chính sách]    │
└─────────────────────────────────┘
```

**Thành phần XML chính:**
- `BottomSheetDialogFragment` với `peekHeight = wrap_content`
- `View` drag handle — chiều rộng 40dp, cao 4dp, bo góc, màu `@color/surface_variant`
- `ImageView` — logo ứng dụng, kích thước 64x64dp, căn giữa
- `TextView` — tiêu đề "Đăng nhập để tiếp tục", `textAppearance = @style/TextAppearance.Material3.HeadlineSmall`
- 3 `MaterialButton` đăng nhập mạng xã hội:
  - `style = @style/Widget.Material3.Button.OutlinedButton`
  - `app:icon` = logo tương ứng (Google/Facebook/Apple)
  - `app:iconGravity = "textStart"`
  - `app:iconSize = 20dp`
  - `app:cornerRadius = 8dp`
  - Chiều cao: 52dp, rộng: `match_parent`
- `View` divider ngang với `TextView` "hoặc" ở giữa
- `TextInputLayout` với `TextInputEditText` — email/số điện thoại
  - `style = @style/Widget.Material3.TextInputLayout.OutlinedBox`
  - `android:hint = "Email hoặc số điện thoại"`
  - `app:startIconDrawable = "@drawable/ic_email"`
- `TextView` dạng link "Chưa có tài khoản? Đăng ký" — `SpannableString` để underline phần "Đăng ký"
- `TextView` điều khoản/chính sách — `textSize = 11sp`, `textColor = @color/on_surface_variant`

**Style & màu sắc:**
- Background BottomSheet: `@color/surface`, `cornerRadius = 28dp` (top left + top right)
- Nền app mờ phía sau: `backgroundDimAmount = 0.5`
- Padding nội dung: `24dp` tất cả các phía

###  Tương Tác Người Dùng

| Hành động | Phản hồi UI |
|---|---|
| Kéo handle xuống | Sheet thu lại, đóng |
| Nhấn vào nền mờ | Sheet đóng |
| Nhấn "Tiếp tục với Google" | Ripple effect → mở Google Sign-In Intent (mock: Toast "Đăng nhập Google") |
| Nhấn "Tiếp tục với Facebook" | Ripple effect → mở Facebook Login (mock: Toast) |
| Nhấn field Email | Bàn phím hiện ra, label nổi lên (Floating Label animation) |
| Nhấn "Đăng ký" (link) | Navigate sang `SignUpFragment` |
| Sheet mở hoàn toàn | Animation slide-up 300ms với `BottomSheetBehavior` |

###  Java Class

**File:** `LoginBottomSheetFragment.java`  
**Package:** `com.app.auth`

```java
// Cấu trúc class
public class LoginBottomSheetFragment extends BottomSheetDialogFragment {
    private FragmentLoginBottomSheetBinding mBinding;

    @Override
    public View onCreateView(...) {
        mBinding = FragmentLoginBottomSheetBinding.inflate(inflater);
        setupClickListeners();
        return mBinding.getRoot();
    }

    private void setupClickListeners() {
        mBinding.btnGoogle.setOnClickListener(v -> loginWithGoogle());
        mBinding.btnFacebook.setOnClickListener(v -> loginWithFacebook());
        mBinding.btnEmail.setOnClickListener(v -> navigateToEmailLogin());
        mBinding.tvSignUp.setOnClickListener(v -> navigateToSignUp());
    }

    // Gọi từ màn hình khác:
    // LoginBottomSheetFragment.newInstance().show(getSupportFragmentManager(), "login");
}
```

---

## 2. Màn Hình Đăng Ký (Sign Up)

###  Mô Tả Chức Năng
Cho phép người dùng tạo tài khoản mới bằng email + mật khẩu. Có validation inline — lỗi hiển thị ngay dưới từng ô nhập khi người dùng rời focus, không chờ submit. Sử dụng `Fragment` thay vì `Activity` để phù hợp kiến trúc Single-Activity.

###  Giao Diện (Layout XML)

**File:** `fragment_sign_up.xml`

```
┌─────────────────────────────────┐
│ ←  Đăng ký tài khoản            │
│                                  │
│   Logo nhỏ + "Bắt đầu hành      │
│    trình tri thức của bạn"       │
│                                  │
│  Họ và tên *                     │
│  ┌──────────────────────────┐    │
│  │   Nguyễn Văn A         │    │
│  └──────────────────────────┘    │
│                                  │
│  Email *                         │
│  ┌──────────────────────────┐    │
│  │  example@email.com    │    │
│  └──────────────────────────┘    │
│  ⚠ Email không hợp lệ           │
│                                  │
│  Mật khẩu *                      │
│  ┌──────────────────────────┐    │
│  │   ••••••••         👁  │    │
│  └──────────────────────────┘    │
│  [====----] Độ mạnh: Trung bình  │
│                                  │
│  Xác nhận mật khẩu *             │
│  ┌──────────────────────────┐    │
│  │   ••••••••         👁  │    │
│  └──────────────────────────┘    │
│                                  │
│  ☐ Tôi đồng ý Điều khoản dịch   │
│    vụ & Chính sách bảo mật       │
│                                  │
│  ┌──────────────────────────┐    │
│  │      TẠO TÀI KHOẢN       │    │
│  └──────────────────────────┘    │
│                                  │
│  Đã có tài khoản? [Đăng nhập]   │
└─────────────────────────────────┘
```

**Thành phần XML chính:**
- `MaterialToolbar` với back arrow và tiêu đề
- 4 `TextInputLayout` kiểu `OutlinedBox`:
  - Họ tên: `inputType = textPersonName`, icon người dùng
  - Email: `inputType = textEmailAddress`, icon email
  - Mật khẩu: `inputType = textPassword`, icon khóa + `endIconMode = password_toggle`
  - Xác nhận mật khẩu: tương tự
- `LinearProgressIndicator` hiển thị độ mạnh mật khẩu (ẩn mặc định, hiện khi nhập)
  - 3 mức: Yếu (đỏ) / Trung bình (vàng) / Mạnh (xanh)
- `MaterialCheckBox` + `TextView` SpannableString cho điều khoản
- `MaterialButton` "Tạo tài khoản" — `style = FilledButton`, `cornerRadius = 12dp`, chiều cao 56dp
- `TextView` link "Đã có tài khoản? Đăng nhập"

###  Tương Tác Người Dùng

| Hành động | Phản hồi UI |
|---|---|
| Nhấn back arrow | Navigate về màn hình trước |
| Rời focus ô Email | Validate ngay → hiện error text đỏ nếu sai định dạng |
| Nhập mật khẩu | `LinearProgressIndicator` hiện dần với màu theo độ mạnh |
| Nhấn icon 👁 mật khẩu | Toggle hiển thị/ẩn ký tự mật khẩu |
| Nhấn "Tạo tài khoản" khi form chưa đủ | Nút rung nhẹ (shake animation) + scroll đến field lỗi đầu tiên |
| Nhấn "Tạo tài khoản" khi form hợp lệ | Button chuyển sang trạng thái loading (CircularProgressIndicator nhỏ) |
| Tạo thành công (mock) | Navigate sang `HomeFragment`, BottomNavBar hiện ra |

###  Java Class

**File:** `SignUpFragment.java`  
**Package:** `com.app.auth`

```java
public class SignUpFragment extends Fragment {
    private FragmentSignUpBinding mBinding;

    private void validateAndSubmit() {
        boolean isValid = true;
        String email = mBinding.tilEmail.getEditText().getText().toString().trim();
        String password = mBinding.tilPassword.getEditText().getText().toString();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mBinding.tilEmail.setError("Email không hợp lệ");
            isValid = false;
        } else {
            mBinding.tilEmail.setError(null);
        }

        if (password.length() < 8) {
            mBinding.tilPassword.setError("Mật khẩu tối thiểu 8 ký tự");
            isValid = false;
        }

        if (isValid) {
            showLoadingState(true);
            // Gọi AuthViewModel.register(email, password)
        }
    }

    private void updatePasswordStrength(String password) {
        int strength = calculateStrength(password);
        mBinding.passwordStrengthIndicator.setProgress(strength, true);
    }
}
```

---

## 3. Trang Chủ (Books Hub)

###  Mô Tả Chức Năng
Màn hình trung tâm của ứng dụng — nơi người dùng khám phá và tìm kiếm nội dung mới. Phân tách **Sách nói** và **Ebook** thành 2 tab riêng biệt thông qua `ViewPager2 + TabLayout`. Mỗi tab hiển thị nhiều băng chuyền (carousel) nội dung theo chiều ngang, xếp dọc.

### 🎨 Giao Diện (Layout XML)

**File:** `fragment_home.xml`

```
┌─────────────────────────────────┐
│ Ứng dụng     🔔  👤          │  ← AppBar
│                                  │
│ "Xin chào, Minh "             │
│ "Hôm nay bạn muốn nghe gì?"     │
│                                  │
│ [Sách nói] [Ebook] [Tóm tắt]   │  ← TabLayout
│ ─────────                        │
│                                  │
│  TOP EBOOK THỊNH HÀNH         │
│ ┌────┐ ┌────┐ ┌────┐ ┌────┐ →  │  ← RecyclerView ngang
│ │ 📖 │ │ 📖 │ │ 📖 │ │ 📖 │    │
│ │    │ │    │ │    │ │    │    │
│ └────┘ └────┘ └────┘ └────┘    │
│ Tên sách  Tên sách               │
│                                  │
│  QUÀ TẶNG TRONG NGÀY         │
│ ┌─────────────────────────────┐  │  ← Banner lớn
│ │  📚 Sách miễn phí hôm nay!  │  │
│ └─────────────────────────────┘  │
│                                  │
│  DANH MỤC                     │
│ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ →    │  ← RecyclerView ngang (icon)
│ │🧘│ │💼│ │👶│ │📖│ │🎵│      │
│ └──┘ └──┘ └──┘ └──┘ └──┘      │
│                                  │
│  MỚI XUẤT BẢN                 │
│ ┌────┐ ┌────┐ ┌────┐ ┌────┐ →  │
│ │ 📖 │ │ 📖 │ │ 📖 │ │ 📖 │    │
│ └────┘ └────┘ └────┘ └────┘    │
│                                  │
│  BÁN CHẠY NHẤT                │
│ ... (carousel tương tự)         │
│                                  │
└─────────────────────────────────┘
│ 📚  🔍  📁  👤 │  ← BottomNavBar
```

**Thành phần XML chính:**

**`fragment_home.xml` (container):**
- `CoordinatorLayout` làm root
- `AppBarLayout` + `MaterialToolbar`:
  - Logo/tên app bên trái
  - `IconButton` thông báo (badge đỏ nếu có thông báo mới)
  - `ShapeableImageView` avatar người dùng — 32x32dp, bo tròn
- `TextView` lời chào — "`textAppearance = HeadlineMedium`"
- `TabLayout` + `ViewPager2`:
  - `tabMode = scrollable`
  - `tabGravity = start`
  - `tabIndicatorColor = @color/primary`
  - `tabTextColor = @color/on_surface_variant`
  - `tabSelectedTextColor = @color/primary`
- `RecyclerView` dọc chính với `LinearLayoutManager(VERTICAL)`

**`item_section_carousel.xml` (mỗi section):**
- `TextView` tiêu đề section (emoji + ALL CAPS)
- `TextView` "Xem tất cả" — link màu primary, align right
- `RecyclerView` ngang với `LinearLayoutManager(HORIZONTAL)`

**`item_book_card.xml` (mỗi thẻ sách):**
- `MaterialCardView`:
  - `cardCornerRadius = 8dp`
  - `cardElevation = 4dp`
  - Kích thước: 120x180dp (tỷ lệ 2:3 cho ebook), 120x120dp (1:1 cho audiobook)
- `ShapeableImageView` bìa sách — Glide load với placeholder gradient
- `TextView` tên sách — tối đa 2 dòng, `ellipsize = end`
- `TextView` tên tác giả — 1 dòng, màu mờ hơn
- Badge "PREMIUM" (nếu có) — `MaterialCardView` nhỏ góc trên phải, màu gradient cam→đỏ

**Nested RecyclerView tối ưu:**
```java
// Dùng chung RecycledViewPool
RecyclerView.RecycledViewPool sharedPool = new RecyclerView.RecycledViewPool();
sharedPool.setMaxRecycledViews(0, 10); // ViewType 0, tối đa 10 items cache

// Mỗi RecyclerView ngang dùng chung pool
innerRecyclerView.setRecycledViewPool(sharedPool);
innerRecyclerView.setItemViewCacheSize(5);
```

### 🖱️ Tương Tác Người Dùng

| Hành động | Phản hồi UI |
|---|---|
| Vuốt ngang giữa các tab | `ViewPager2` transition mượt mà, `TabLayout` indicator trượt |
| Nhấn tab | Switch ngay lập tức, không reload nếu đã load |
| Cuộn dọc | `AppBarLayout` collapse dần, TabLayout ghim lại trên đầu |
| Cuộn ngang carousel | Scroll ngang với snap (LinearSnapHelper) |
| Nhấn thẻ sách | Ripple → Navigate sang `BookDetailFragment` |
| Nhấn "Xem tất cả" | Navigate sang màn hình danh sách đầy đủ |
| Pull-to-refresh | `SwipeRefreshLayout` → reload dữ liệu |
| Nhấn banner "Quà tặng" | Navigate sang màn hình chi tiết ưu đãi |
| Nhấn icon danh mục | Navigate sang `DiscoveryFragment` với danh mục đã chọn pre-selected |

###  Java Class

**Files:**
- `HomeFragment.java` — `com.app.home`
- `BookCarouselAdapter.java` — adapter RecyclerView ngang
- `HomeSectionAdapter.java` — adapter RecyclerView dọc (multi-viewtype)

---

## 4. Khám Phá (Discovery)

###  Mô Tả Chức Năng
Công cụ tìm kiếm chủ động và phân loại nội dung. Người dùng có thể tìm kiếm theo tên sách, tác giả, mentor; hoặc duyệt qua các danh mục trực quan. Khi nhập tìm kiếm, UI chuyển trạng thái sang chế độ tìm kiếm với lịch sử và gợi ý tức thì.

###  Giao Diện (Layout XML)

**File:** `fragment_discovery.xml`

**Trạng thái mặc định (Browse mode):**
```
┌─────────────────────────────────┐
│  Khám Phá                        │
│                                  │
│  ┌──────────────────────────────┐│
│  │ 🔍  Tìm tên sách, tác giả...││  ← SearchBar (nổi)
│  └──────────────────────────────┘│
│                                  │
│   THỂ LOẠI                    │
│                                  │
│  ┌─────────┐  ┌─────────┐       │
│  │ 🎧       │  │ 📖       │      │
│  │ Sách Nói │  │ Ebook   │       │  ← Grid 2 cột
│  └─────────┘  └─────────┘       │
│  ┌─────────┐  ┌─────────┐       │
│  │ 🧘       │  │ 👶       │      │
│  │ Thiền    │  │ Thiếu nhi│      │
│  └─────────┘  └─────────┘       │
│  ┌─────────┐  ┌─────────┐       │
│  │ 💼       │  │ 📝       │      │
│  │ Kinh doanh│ │ Tóm tắt │      │
│  └─────────┘  └─────────┘       │
│  ┌─────────┐  ┌─────────┐       │
│  │ 🎵       │  │ 🌍       │      │
│  │ Podcast  │  │ Tiếng Anh│      │
│  └─────────┘  └─────────┘       │
└─────────────────────────────────┘
```

**Trạng thái tìm kiếm (Search mode):**
```
┌─────────────────────────────────┐
│  ← ┌──────────────────────────┐ │
│    │ 🔍  Đắc Nhân Tâm         │ │  ← Focused
│    └──────────────────────────┘ │
│                                  │
│  🕐 TÌM KIẾM GẦN ĐÂY           │
│  Đắc Nhân Tâm               ✕  │
│  Thiền định                  ✕  │
│  Harari                      ✕  │
│                                  │
│  💡 GỢI Ý CHO BẠN              │
│  ┌──────────────────────────┐   │
│  │ 📖 Đắc Nhân Tâm — Sách nói│  │  ← Gợi ý tức thì
│  └──────────────────────────┘   │
│  ┌──────────────────────────┐   │
│  │ 📖 Đắc Nhân Tâm — Ebook  │  │
│  └──────────────────────────┘   │
└─────────────────────────────────┘
```

**Thành phần XML chính:**

- `SearchBar` (Material 3) — nổi phía trên:
  - `android:hint = "Tìm tên sách, tác giả, mentor..."`
  - `app:navigationIcon = "@drawable/ic_search"`
  - `elevation = 6dp`
  - `cornerRadius = 28dp` (pill shape)
- `SearchView` ẩn — hiện khi focus vào SearchBar
- `RecyclerView` với `GridLayoutManager(context, 2)`:
  - `spanSizeLookup` cho phép header chiếm full width
  - `addItemDecoration` để tạo spacing 12dp giữa các ô

**`item_category_card.xml` (mỗi ô danh mục):**
- `MaterialCardView` — `cardCornerRadius = 16dp`
- Background: `GradientDrawable` với 2 màu gradient (mỗi danh mục một bộ màu riêng)
- `ImageView` icon emoji/vector — 36x36dp
- `ImageView` hình minh họa nhỏ góc dưới phải — mờ 40%, kích thước 80x80dp
- `TextView` tên danh mục — màu trắng, `textStyle = bold`

**Màu gradient từng danh mục:**
```xml
<!-- Sách Nói: cam -->     #FF6B35 → #FF8C42
<!-- Ebook: xanh lá -->    #56AB2F → #A8E063
<!-- Thiền: tím nhạt -->   #8E2DE2 → #4A00E0
<!-- Thiếu nhi: hồng -->   #FF6B9D → #C44B8A
<!-- Kinh doanh: xanh --> #1A237E → #283593
<!-- Tóm tắt: vàng -->    #F7971E → #FFD200
```

### 🖱️ Tương Tác Người Dùng

| Hành động | Phản hồi UI |
|---|---|
| Nhấn SearchBar | Expand animation → hiện SearchView + lịch sử + bàn phím |
| Gõ vào SearchView | Debounce 300ms → hiển thị gợi ý tức thì bên dưới |
| Nhấn X xóa lịch sử | Remove item khỏi list với slide animation |
| Nhấn gợi ý | Navigate sang `BookDetailFragment` |
| Enter / Search | Navigate sang màn hình kết quả tìm kiếm |
| Nhấn ô danh mục | Ripple elevation → Navigate sang danh sách sách theo danh mục |
| Nhấn back khi đang search | Collapse SearchView, trở về browse mode |

### 🔧 Java Class

**Files:**
- `DiscoveryFragment.java` — `com.app.discovery`
- `CategoryAdapter.java` — GridLayoutManager adapter
- `SearchSuggestionAdapter.java` — gợi ý tìm kiếm

---

## 5. Thư Viện (Library)

###  Mô Tả Chức Năng
Không gian cá nhân hóa — tủ sách kỹ thuật số riêng của người dùng. Quản lý toàn bộ nội dung: đã mua, đã tải, yêu thích, đánh dấu, và lịch sử đọc/nghe. Hiển thị tiến trình đọc theo phần trăm cho từng item.
###  Giao Diện (Layout XML)

**File:** `fragment_library.xml`

```
┌─────────────────────────────────┐
│  Thư Viện           🔍  ≡       │
│                                  │
│ [Gần đây][Đã mua][Đã tải][❤️]  │  ← TabLayout (scrollable)
│ ──────                           │
│                                  │
│  ── TAB "GẦN ĐÂY" ──           │
│                                  │
│  ┌───────────────────────────┐  │
│  │ 🖼️ │ Đắc Nhân Tâm         │  │
│  │     │ Dale Carnegie        │  │  ← Item đang đọc dở
│  │     │ ████████░░  78%      │  │
│  │     │ ⏱ Còn 1 giờ 23 phút │  │
│  └───────────────────────────┘  │
│                                  │
│  ┌───────────────────────────┐  │
│  │ 🖼️ │ Sapiens               │  │
│  │     │ Yuval Noah Harari    │  │
│  │     │ ███░░░░░░░  32%      │  │
│  │     │ ⏱ Còn 5 giờ 10 phút │  │
│  └───────────────────────────┘  │
│                                  │
│  ── TAB "ĐÃ TẢI" (Empty State) ──│
│                                  │
│        📦                        │
│   Chưa có sách nào được tải     │
│   Tải sách để nghe offline!     │
│                                  │
│  ┌──────────────────────────┐   │
│  │    Khám phá ngay         │   │
│  └──────────────────────────┘   │
│                                  │
└─────────────────────────────────┘
```

**Thành phần XML chính:**

- `MaterialToolbar` với tên "Thư Viện" + icon search + icon filter
- `TabLayout` + `ViewPager2`:
  - Tabs: Gần đây / Đã mua / Đã tải / Yêu thích / Đánh dấu
  - `tabMode = scrollable`
- Mỗi tab là 1 Fragment riêng: `RecentFragment`, `PurchasedFragment`...

**`item_library_book.xml`:**
- `ConstraintLayout` horizontal layout
- `ShapeableImageView` bìa sách — 70x100dp, `cornerRadius = 6dp`
- `TextView` tên sách — `textStyle = bold`, tối đa 2 dòng
- `TextView` tên tác giả — màu mờ
- `LinearProgressIndicator` (Material 3):
  - Chiều cao 6dp
  - `trackColor = @color/surface_variant`
  - `indicatorColor = @color/primary`
- `TextView` phần trăm + thời gian còn lại
- Badge tải về (icon download) ở góc — ẩn/hiện theo trạng thái

**`fragment_empty_state.xml` (dùng chung):**
- `LottieAnimationView` — animation mascot đội hộp (file `empty_library.json`)
- `TextView` tiêu đề "Chưa có nội dung nào"
- `TextView` mô tả phụ
- `MaterialButton` "Khám phá ngay"

**State Management trong Java:**
```java
private void updateViewState(List<Book> books) {
    if (books == null || books.isEmpty()) {
        mBinding.recyclerView.setVisibility(View.GONE);
        mBinding.emptyStateLayout.setVisibility(View.VISIBLE);
    } else {
        mBinding.recyclerView.setVisibility(View.VISIBLE);
        mBinding.emptyStateLayout.setVisibility(View.GONE);
        mAdapter.submitList(books);
    }
}
```

###  Tương Tác Người Dùng

| Hành động | Phản hồi UI |
|---|---|
| Vuốt giữa các tab | `ViewPager2` smooth transition |
| Nhấn item sách | Navigate sang `AudioPlayerFragment` hoặc `EbookReaderActivity` tùy type |
| Long press item | Hiện `ContextMenu` (Tải xuống / Xóa / Chia sẻ) |
| Nhấn icon delete | Confirm dialog → remove item với slide-out animation |
| Nhấn "Khám phá ngay" (empty state) | Navigate sang `DiscoveryFragment` |
| Pull-to-refresh | Sync tiến trình mới nhất từ local DB |
| Nhấn icon filter | Hiện `BottomSheetDialog` với options sắp xếp |

---

## 6. Chi Tiết Sách (Book Detail)

###  Mô Tả Chức Năng
Màn hình hiển thị toàn bộ thông tin về một cuốn sách: bìa, mô tả, tác giả, đánh giá. Là điểm quyết định chuyển đổi — người dùng nghe thử hoặc mua tại đây.

###  Giao Diện (Layout XML)

**File:** `fragment_book_detail.xml`

```
┌─────────────────────────────────┐
│ ←                       ⋮     │  ← Toolbar trong suốt khi scroll
│                                  │
│ ┌─────────────────────────────┐ │
│ │                             │ │
│ │         📖 BÌA SÁCH         │ │  ← Hero image (full width, 280dp)
│ │         (lớn, đẹp)          │ │
│ │                             │ │
│ └─────────────────────────────┘ │
│                                  │
│ Đắc Nhân Tâm                    │
│ ⭐⭐⭐⭐⭐ 4.8  (2,341 đánh giá) │
│ Dale Carnegie · NXB Trẻ         │
│                                  │
│ 🎧 Sách nói · 8g 23p  📖 Ebook  │
│                                  │
│ ┌────────────────┐ ┌──────────┐ │
│ │ ▶ NGHE THỬ    │ │ MUA 89k  │ │
│ └────────────────┘ └──────────┘ │
│                                  │
│ ── MÔ TẢ ──                     │
│ Trong cuốn sách kinh điển        │
│ này, Dale Carnegie...            │
│ [Xem thêm]                      │
│                                  │
│ ── TÁC GIẢ ──                   │
│ 👤  Dale Carnegie                │
│ Tác giả bestseller #1...         │
│                                  │
│ ── ĐÁNH GIÁ (2,341) ──          │
│ ⭐⭐⭐⭐⭐  "Tuyệt vời..."        │
│ ⭐⭐⭐⭐    "Rất hay..."          │
│ [Xem tất cả đánh giá]           │
│                                  │
│ ── SÁCH TƯƠNG TỰ ──             │
│ ┌────┐ ┌────┐ ┌────┐ →          │
│ │ 📖 │ │ 📖 │ │ 📖 │           │
└─────────────────────────────────┘
```

**Thành phần XML chính:**

- Root: `CoordinatorLayout`
- `AppBarLayout` + `CollapsingToolbarLayout`:
  - `contentScrim = @color/surface` — khi scroll lên sẽ đổi màu
  - `expandedTitleTextAppearance = @style/...HeadlineLarge`
  - Toolbar trong suốt khi mở rộng, đổi màu khi collapse
- `AppCompatImageView` hero image — `scaleType = centerCrop`, chiều cao 280dp
- `NestedScrollView` cho toàn bộ nội dung bên dưới
- `TextView` tên sách — `HeadlineMedium`
- `RatingBar` (không interactive, `isIndicator = true`) + `TextView` điểm + số lượng
- `ChipGroup` horizontal — loại format (Sách nói / Ebook) + thời lượng
- 2 `MaterialButton`:
  - "Nghe thử" — `OutlinedButton`, icon play
  - "Mua ngay 89k" — `FilledButton`, màu primary
- `TextView` mô tả với tính năng expand/collapse (max 4 dòng + "Xem thêm")
- `MaterialCardView` thông tin tác giả — row ngang với avatar + tên + bio
- `RecyclerView` đánh giá — 3 item preview
- `RecyclerView` ngang sách tương tự

###  Tương Tác Người Dùng

| Hành động | Phản hồi UI |
|---|---|
| Cuộn lên | `CollapsingToolbarLayout` collapse, Toolbar chuyển màu, tiêu đề nhỏ lại |
| Nhấn  | Icon toggle filled/outline với bounce animation + Toast "Đã thêm vào yêu thích" |
| Nhấn "Nghe thử" | Mở mini audio player dưới màn hình (30 giây demo) |
| Nhấn "Mua ngay" | Navigate sang màn hình thanh toán |
| Nhấn "Xem thêm" (mô tả) | Expand text với animation cao dần, "Xem thêm" → "Thu gọn" |
| Nhấn thẻ tác giả | Navigate sang màn hình danh sách sách của tác giả |
| Nhấn "Xem tất cả đánh giá" | Navigate sang màn hình đánh giá đầy đủ |

---

## 7. Trình Phát Audio (Audio Player)

###  Mô Tả Chức Năng
Màn hình tiêu thụ nội dung chính cho sách nói và podcast. Trọng tâm là trải nghiệm điều khiển ergonomic — người dùng thường dùng khi đang làm việc khác (lái xe, tập thể dục). Các nút phải đủ lớn (tối thiểu 48dp). Phát nhạc chạy trong `ExoPlayer` + `Foreground Service` — không dừng khi thu nhỏ app.

### Giao Diện (Layout XML)

**File:** `fragment_audio_player.xml`

```
┌─────────────────────────────────┐
│ ∨  Chương 3: Biết lắng nghe  ⋮ │  ← Dismiss + menu
│                                  │
│                                  │
│         ┌───────────┐            │
│         │           │            │
│         │  📖 BÌA   │            │  ← ImageView lớn, bo góc 16dp
│         │  (SÁCH)   │            │     Kích thước: 280x280dp
│         │           │            │     Đổ bóng đậm phía dưới
│         └───────────┘            │
│                                  │
│ Đắc Nhân Tâm                   │
│ Dale Carnegie                    │
│                                  │
│ 00:23:45 ══════●════════ 08:23:10│  ← SeekBar + timestamps
│                                  │
│     ⏮  ⏪15  ▶/⏸  15⏩  ⏭      │  ← 5 controls
│   [prev][back][PLAY][fwd][next]  │
│                                  │
│  1.0x  ·  💤 Hẹn giờ  ·  📋 MLC │  ← Tính năng nâng cao
│                                  │
└─────────────────────────────────┘
```

**Màu nền động:** Background màn hình tự động đổi màu gradient theo màu chủ đạo của bìa sách (Android Palette API).

**Thành phần XML chính:**

- Root: `ConstraintLayout` với background `GradientDrawable` (cập nhật động qua Java)
- `ImageView` toolbar "∨" (chevron down) để dismiss — nếu mở từ mini player
- `TextView` tiêu đề chương — `titleTextAppearance = TitleMedium`, căn giữa
- `ImageButton` menu "⋮" — mở `BottomSheetDialog` cài đặt nâng cao
- `ShapeableImageView` bìa sách:
  - `shapeAppearanceModel = cornerSize(16dp)`
  - `elevation = 24dp` (đổ bóng đẹp)
  - Kích thước: `280dp x 280dp`
  - Animation scale-up nhẹ khi đang phát, scale-down khi pause
- `TextView` tên sách + tên tác giả
- `ImageButton` yêu thích ❤️
- `SeekBar`:
  - `progressTintList = @color/primary`
  - `thumbTint = @color/primary`
  - `trackHeight = 4dp`
- `TextView` thời gian hiện tại (left) + tổng thời lượng (right)
- 5 `ImageButton` controls:
  - Previous Chapter: 36x36dp
  - Back 15s: 40x40dp
  - Play/Pause: **64x64dp** (lớn nhất), `MaterialButton` hình tròn filled
  - Forward 15s: 40x40dp
  - Next Chapter: 36x36dp
- `MaterialTextButton` tốc độ phát "1.0x"
- `MaterialTextButton` hẹn giờ ngủ "💤"
- `MaterialTextButton` mục lục "📋"

**BottomSheet cài đặt nâng cao (`dialog_player_settings.xml`):**
```
Tốc độ phát:   [0.75x] [1.0x] [1.25x] [1.5x] [2.0x]
                         ●

Hẹn giờ tắt:   Off | 15p | 30p | 45p | 60p | Hết chương
                 ●

Chất lượng:    Tiết kiệm (64kbps)  ●  Chuẩn (128kbps)  Cao (320kbps)
```

###  Tương Tác Người Dùng

| Hành động | Phản hồi UI |
|---|---|
| Nhấn Play | Bìa scale-up 1.0→1.03 với spring animation, icon đổi thành Pause |
| Nhấn Pause | Bìa scale-down về 1.0, icon đổi thành Play |
| Kéo SeekBar | Hiện popup timestamp phía trên ngón tay (custom SeekBar) |
| Thả SeekBar | `ExoPlayer.seekTo(position)` — jump đến vị trí mới |
| Nhấn Back 15s | Icon flash animation, seek lùi 15 giây |
| Nhấn Forward 15s | Icon flash animation, seek tiến 15 giây |
| Nhấn "1.0x" | Hiện chip selector tốc độ phát |
| Nhấn "💤" | Hiện chip selector hẹn giờ, đếm ngược hiển thị |
| Nhấn "∨" (dismiss) | Collapse xuống thành Mini Player ở bottom |
| Vuốt xuống | Collapse gesture — thu nhỏ thành mini player |
| Màn hình khóa | Media Notification hiện với controls Play/Pause/Skip |

### 🔧 Java Class

**Files:**
- `AudioPlayerFragment.java` — UI only
- `PlaybackService.java` (Foreground Service + ExoPlayer) — tuần sau

```java
// SeekBar cập nhật tự động từ Service
private Handler mSeekBarHandler = new Handler(Looper.getMainLooper());
private Runnable mUpdateSeekBar = new Runnable() {
    @Override
    public void run() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            long currentPos = mPlayer.getCurrentPosition();
            mBinding.seekBar.setProgress((int) currentPos);
            mBinding.tvCurrentTime.setText(formatTime(currentPos));
        }
        mSeekBarHandler.postDelayed(this, 500); // cập nhật mỗi 500ms
    }
};

// Palette API — đổi màu nền theo bìa sách
Bitmap bitmap = ((BitmapDrawable) mBinding.ivCover.getDrawable()).getBitmap();
Palette.from(bitmap).generate(palette -> {
    int dominantColor = palette.getDominantColor(Color.DKGRAY);
    int darkerColor = ColorUtils.blendARGB(dominantColor, Color.BLACK, 0.6f);
    GradientDrawable gradient = new GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        new int[]{dominantColor, darkerColor}
    );
    mBinding.rootLayout.setBackground(gradient);
});
```

---

## 8. Trình Đọc Ebook (Ebook Reader)

###  Mô Tả Chức Năng
Môi trường đọc sâu — loại bỏ mọi phân tâm. Tích hợp thư viện **FolioReader-Android** làm lõi EPUB. Giao diện bao bọc bên ngoài đồng nhất với nhận diện thương hiệu ứng dụng. Hỗ trợ Night Mode, điều chỉnh font/cỡ chữ, highlight và ghi chú.

###  Giao Diện (Layout XML)

**Trạng thái đọc bình thường (UI ẩn):**
```
┌─────────────────────────────────┐
│                                  │
│   Trong cuốn sách kinh điển      │
│   này, Dale Carnegie đã đúc      │
│   kết những nguyên tắc vàng      │
│   về cách đối nhân xử thế.      │
│                                  │
│   Nguyên tắc đầu tiên...        │
│                                  │
│   ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓       │
│                                  │
└─────────────────────────────────┘
```

**Khi chạm giữa màn hình (UI hiện ra):**
```
┌─────────────────────────────────┐
│ ←  Đắc Nhân Tâm            📑  │  ← Toolbar mờ dần (fadeIn 200ms)
│                                  │
│   [Văn bản đang đọc...]         │
│                                  │
│                                  │
│ ─── Chương 3 / 12 ─── 45% ───  │
│ ████████████████░░░░░░░░░░░░    │  ← Thanh tiến trình dưới
│ Tr. 124         ⏱ ~23 phút còn │  ← Trang + ETA
│                                  │
│  Aa  ≡  ←→                  │  ← Quick controls
└─────────────────────────────────┘
```

**Khi long press chọn văn bản:**
```
┌─────────────────────────────────┐
│                                  │
│   "...những nguyên tắc          │
│   ████████████████████          │  ← Văn bản được chọn (highlight xanh)
│   ████████████████████          │
│   vàng về cách..."              │
│                                  │
│   ┌─────────────────────────┐   │
│   │ 🟡 Vàng  🟢 Xanh  🔴 Đỏ│   │  ← Popup highlight màu
│   │ 📝 Ghi chú  📖 Từ điển │   │
│   └─────────────────────────┘   │
└─────────────────────────────────┘
```

**Panel cài đặt hiển thị (`fragment_reader_settings.xml`):**
```
┌─────────────────────────────────┐
│                                  │
│  ☀️ Độ sáng              🌙     │
│  ─────────●──────────────────   │  ← SeekBar
│                                  │
│  Font chữ                        │
│  [Georgia] [Roboto] [OpenDyslexic] │  ← RecyclerView ngang
│                                  │
│  Cỡ chữ                          │
│  A-    ─────────●────────   A+  │  ← SeekBar
│        Preview: "Văn bản mẫu"   │
│                                  │
│  Chế độ                          │
│  [☀ Ngày] [🌙 Đêm] [📜 Sepia]  │
│                                  │
└─────────────────────────────────┘
```

**Thành phần XML chính:**

- Root: `RelativeLayout` (để overlay UI lên FolioReader WebView)
- `FolioReader` WebView — chiếm toàn màn hình
- `MaterialToolbar` — `visibility = GONE` mặc định, `VISIBLE` khi tap
  - `animate().alpha(1f).setDuration(200)` khi hiện
  - `animate().alpha(0f).setDuration(200)` khi ẩn
- Thanh tiến trình dưới:
  - `LinearProgressIndicator` — chiều cao 3dp, full width
  - `TextView` số trang hiện tại/tổng
  - `TextView` thời gian đọc ước tính còn lại
- Quick control bar:
  - `ImageButton` Night Mode toggle
  - `ImageButton` Cài đặt font (mở bottom sheet)
  - `ImageButton` Mục lục (mở side drawer)
  - `ImageButton` Flip left/right (chuyển chế độ scroll/page)

###  Tương Tác Người Dùng

| Hành động | Phản hồi UI |
|---|---|
| Tap giữa màn hình | Toolbar + bottom bar FadeIn (200ms) |
| Tap lần 2 (hoặc chờ 3s) | Toolbar + bottom bar FadeOut |
| Vuốt ngang | Chuyển trang (page-flip animation) |
| Vuốt dọc | Scroll liên tục (nếu chế độ scroll) |
| Long press văn bản | FolioReader callback → hiện popup highlight/ghi chú |
| Chọn màu highlight | Apply highlight, lưu vào Room DB |
| Kéo SeekBar tiến trình | Popup hiện số chương + thời gian → thả để jump |
| Nhấn icon 🌙 | Fade chuyển Night Mode (nền tối, chữ sáng) |
| Nhấn "Aa" | Bottom sheet cài đặt hiện lên |
| Thay đổi cỡ chữ | Preview text cập nhật realtime trong bottom sheet |
| Nhấn icon Mục lục | Side drawer trượt từ trái ra |
| Back | Navigate về màn hình trước, lưu vị trí đọc cuối vào Room DB |

---

## 9. Hồ Sơ Cá Nhân (Profile)

###  Mô Tả Chức Năng
Không gian cá nhân hóa — hiển thị thông tin người dùng, thống kê đọc/nghe, huy hiệu thành tích, và cài đặt tài khoản. Cũng là điểm chuyển đổi lên gói Premium.

###  Giao Diện (Layout XML)

**File:** `fragment_profile.xml`

```
┌─────────────────────────────────┐
│  Hồ Sơ                    ⚙️   │  ← Toolbar
│                                  │
│  ┌───────────────────────────┐  │
│  │  🟣🟣🟣  (gradient bg)    │  │  ← Header card
│  │                            │  │
│  │     👤  (avatar 80dp)      │  │
│  │   Nguyễn Văn Minh          │  │
│  │   minh@email.com           │  │
│  │                            │  │
│  │  ┌──────┐ ┌──────┐ ┌────┐ │  │
│  │  │  12  │ │  47h │ │ 8  │ │  │  ← Thống kê
│  │  │Sách  │ │Nghe  │ │Huy │ │  │
│  │  │đã đọc│ │tháng │ │hiệu│ │  │
│  │  └──────┘ └──────┘ └────┘ │  │
│  └───────────────────────────┘  │
│                                  │
│  ┌──────────────────────────┐   │
│  │ 👑 NÂNG CẤP LÊN PREMIUM  │   │  ← Banner upgrade (nếu free)
│  │ Nghe không giới hạn!     │   │
│  └──────────────────────────┘   │
│                                  │
│  HUY HIỆU                       │
│  🏅🥇🎯🌟                      │  ← Row icons huy hiệu
│                                  │
│  TÀI KHOẢN                      │
│  👤 Chỉnh sửa thông tin    ›    │
│  🔔 Thông báo               ›    │
│  ⬇️ Quản lý tải xuống      ›    │
│  💳 Gói đăng ký             ›    │
│                                  │
│  KHÁC                           │
│  ⭐ Đánh giá ứng dụng       ›    │
│  ❓ Trợ giúp & Liên hệ     ›    │
│  📋 Điều khoản sử dụng     ›    │
│                                  │
│  [           Đăng xuất         ] │
│                                  │
└─────────────────────────────────┘
```

**Thành phần XML chính:**

- Root: `NestedScrollView`
- **Header card** (`MaterialCardView`, gradient background):
  - Background: `GradientDrawable` dọc từ `@color/primary` → `@color/primary_container`
  - `ShapeableImageView` avatar:
    - Kích thước: 80x80dp, bo tròn hoàn toàn
    - Border 3dp màu trắng (sử dụng `strokeWidth + strokeColor`)
    - Nhấn vào → chọn ảnh từ gallery
  - `TextView` họ tên — trắng, `textStyle = bold`, `HeadlineSmall`
  - `TextView` email — trắng 70% opacity
  - 3 `TextView` thống kê trong `LinearLayout` ngang:
    - Số sách đã đọc / Giờ nghe tháng này / Số huy hiệu
    - Mỗi ô: số lớn đậm + nhãn nhỏ bên dưới, phân cách bằng divider dọc

- **Banner Premium** (`MaterialCardView`):
  - Background gradient cam→vàng
  - Icon , tiêu đề bold, mô tả nhỏ
  - `MaterialButton` "Nâng cấp" — màu trắng, text đậm màu cam
  - Ẩn nếu đã là Premium user

- **Row huy hiệu** (RecyclerView ngang):
  - Mỗi huy hiệu: `MaterialCardView` hình tròn, icon emoji 32dp
  - Huy hiệu chưa mở khóa: grayscale + overlay mờ + icon 🔒

- **Danh sách menu** — mỗi item dùng `item_settings_row.xml`:
  - `ConstraintLayout` horizontal
  - `ImageView` icon màu primary — 24x24dp
  - `TextView` nhãn
  - `ImageView` chevron right "›" — 16x16dp màu mờ
  - `View` divider dưới 1dp
  - Ripple effect khi nhấn

- `MaterialButton` "Đăng xuất" — `OutlinedButton`, `strokeColor = error`, `textColor = error`

### 🖱️ Tương Tác Người Dùng

| Hành động | Phản hồi UI |
|---|---|
| Nhấn avatar | `BottomSheetDialog` với options "Chụp ảnh" / "Chọn từ thư viện" |
| Nhấn "Chỉnh sửa thông tin" | Navigate sang `EditProfileFragment` |
| Nhấn "Thông báo" | Navigate sang `NotificationSettingsFragment` |
| Nhấn banner Premium | Navigate sang màn hình chọn gói đăng ký |
| Nhấn huy hiệu đã mở | `BottomSheetDialog` mô tả chi tiết huy hiệu |
| Nhấn huy hiệu chưa mở | BottomSheet hiển thị "Điều kiện để mở khóa" |
| Nhấn "Đăng xuất" | `AlertDialog` xác nhận → clear session → navigate về Login |
| Nhấn ⚙️ | Navigate sang `SettingsFragment` |

---

## 📐 Hệ Thống Thiết Kế Chung (Design System)

### Màu Sắc (`colors.xml`)
```xml
<color name="primary">#6750A4</color>           <!-- Tím primary -->
<color name="primary_container">#EADDFF</color>
<color name="secondary">#FF6B35</color>          <!-- Cam accent -->
<color name="surface">#FFFBFE</color>
<color name="surface_variant">#E7E0EC</color>
<color name="on_surface">#1C1B1F</color>
<color name="on_surface_variant">#49454F</color>
<color name="error">#B3261E</color>
<color name="gradient_start">#FF6B35</color>     <!-- Gradient CTA -->
<color name="gradient_end">#FF0000</color>
```

### Kích Thước (`dimens.xml`)
```xml
<dimen name="margin_standard">16dp</dimen>
<dimen name="margin_small">8dp</dimen>
<dimen name="margin_large">24dp</dimen>
<dimen name="corner_radius_small">8dp</dimen>
<dimen name="corner_radius_medium">12dp</dimen>
<dimen name="corner_radius_large">16dp</dimen>
<dimen name="corner_radius_full">28dp</dimen>    <!-- Pill shape -->
<dimen name="button_height">52dp</dimen>
<dimen name="touch_target_min">48dp</dimen>
<dimen name="book_card_width">120dp</dimen>
<dimen name="book_card_height_ebook">180dp</dimen>
<dimen name="book_card_height_audio">120dp</dimen>
```

### Typography (`type.xml`)
```xml
<!-- Dùng Material Type Scale -->
HeadlineLarge  → 32sp, weight 400 (tên sách chi tiết)
HeadlineMedium → 28sp, weight 400 (tiêu đề section)
HeadlineSmall  → 24sp, weight 400 (header card profile)
TitleLarge     → 22sp, weight 400
TitleMedium    → 16sp, weight 500 (tên sách trong list)
TitleSmall     → 14sp, weight 500
BodyLarge      → 16sp, weight 400 (nội dung ebook)
BodyMedium     → 14sp, weight 400 (mô tả sách)
LabelLarge     → 14sp, weight 500 (nút bấm)
LabelSmall     → 11sp, weight 500 (badge, caption)
```

### Cấu Trúc Navigation (`mobile_navigation.xml`)
```xml
login_bottom_sheet  →  home_fragment (sau đăng nhập thành công)
home_fragment       →  book_detail_fragment (nhấn thẻ sách)
book_detail_fragment→  audio_player_fragment | ebook_reader_fragment
discovery_fragment  →  book_detail_fragment
library_fragment    →  audio_player_fragment | ebook_reader_fragment
profile_fragment    →  edit_profile_fragment | settings_fragment
```

### Bottom Navigation (5 tabs, dùng `menu_nav.xml`)
```xml
item id="nav_home"       → HomeFragment      icon: ic_home
item id="nav_discovery"  → DiscoveryFragment icon: ic_search
item id="nav_library"    → LibraryFragment   icon: ic_library
item id="nav_profile"    → ProfileFragment   icon: ic_person
```

---

## 📅 Timeline 7 Ngày

| Ngày | Màn hình | Ưu tiên |
|------|----------|---------|
| Ngày 1 | Login + Sign Up | Auth flow |
| Ngày 2 | Trang chủ (Books Hub) — Phức tạp nhất | Core |
| Ngày 3 | Khám phá + Thư viện | Core |
| Ngày 4 | Chi tiết sách | Core |
| Ngày 5 | Audio Player + Ebook Reader — Layout only | Player |
| Ngày 6 | Profile | Profile |
| Ngày 7 | Polish: animation, empty state, responsive | QA |

> **Lưu ý tuần sau (Backend):** Audio Player chỉ cần UI layout tuần này. ExoPlayer + FolioReader + Room DB sẽ kết nối ở tuần 2.
