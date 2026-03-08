# 🚀 Hướng Dẫn Deploy Novel Backend lên Azure VM

## Thông tin VM
| Thông số | Giá trị |
|---|---|
| **VM Name** | backend-server |
| **Size** | Standard_B1ms (1 vCPU, 2GB RAM) |
| **OS** | Ubuntu 24.04 LTS |
| **Region** | East Asia |
| **Username** | reikaikurumi |
| **Disk** | 30GB Premium SSD |

---

## 📋 Mục lục
1. [Cấu hình Azure NSG (Mở port)](#1-cấu-hình-azure-nsg-mở-port)
2. [SSH vào VM](#2-ssh-vào-vm)
3. [Cài đặt Docker trên VM](#3-cài-đặt-docker-trên-vm)
4. [Đưa source code lên VM](#4-đưa-source-code-lên-vm)
5. [Cấu hình biến môi trường](#5-cấu-hình-biến-môi-trường)
6. [Build và chạy](#6-build-và-chạy)
7. [Kiểm tra và quản lý](#7-kiểm-tra-và-quản-lý)
8. [Kết nối DBeaver từ máy local](#8-kết-nối-dbeaver-từ-máy-local)
9. [HTTPS với ngrok (Tuỳ chọn)](#9-https-với-ngrok-tuỳ-chọn)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. Cấu hình Azure NSG (Mở port)

Vào **Azure Portal** → **Resource Group: DoAn-RG** → **Network Security Group** → **Inbound security rules** → **Add**:

| Priority | Name | Port | Protocol | Action |
|---|---|---|---|---|
| 100 | Allow-SSH | 22 | TCP | Allow |
| 110 | Allow-HTTP | 8080 | TCP | Allow |
| 120 | Allow-HTTPS | 443 | TCP | Allow |
| 130 | Allow-Frontend | 3000 | TCP | Allow |

> ⚠️ **Quan trọng**: KHÔNG mở port 5433 (PostgreSQL) ra ngoài internet để đảm bảo bảo mật.

---

## 2. SSH vào VM

Lấy **Public IP** của VM từ Azure Portal, sau đó:

```bash
# Từ máy local (Windows PowerShell)
ssh reikaikurumi@<PUBLIC_IP>
```

Hoặc nếu dùng SSH key:
```bash
ssh -i ~/.ssh/your_key.pem reikaikurumi@<PUBLIC_IP>
```

---

## 3. Cài đặt Docker trên VM

Chạy các lệnh sau trên VM (sau khi SSH):

```bash
# Cập nhật hệ thống
sudo apt update && sudo apt upgrade -y

# Cài đặt Docker
curl -fsSL https://get.docker.com | sudo sh

# Thêm user vào group docker (không cần sudo mỗi lần chạy)
sudo usermod -aG docker $USER

# Cài đặt Docker Compose plugin
sudo apt install docker-compose-plugin -y

# Áp dụng group mới (hoặc logout/login lại)
newgrp docker

# Kiểm tra Docker
docker --version
docker compose version
```

---

## 4. Đưa source code lên VM

### Cách 1: Git clone (Khuyến nghị) ✅

```bash
# Trên VM
cd ~
git clone <YOUR_GITHUB_REPO_URL> novel-backend
cd novel-backend
```

### Cách 2: SCP từ máy local

```powershell
# Từ máy local (Windows PowerShell)
# Nén project trước (loại bỏ build, .gradle, .idea)
cd C:\Users\Mew\Documents\novel-project\novel-backend

# Copy các file cần thiết lên VM
scp -r src/ gradle/ gradlew gradlew.bat build.gradle settings.gradle Dockerfile docker-compose.yml .env.production .dockerignore reikaikurumi@<PUBLIC_IP>:~/novel-backend/
```

### Cách 3: Build JAR trên local rồi copy lên

```powershell
# Trên máy local - Build JAR
cd C:\Users\Mew\Documents\novel-project\novel-backend
.\gradlew bootJar -x test

# Copy JAR + docker files lên VM
scp build/libs/novel-0.0.1-SNAPSHOT.jar reikaikurumi@<PUBLIC_IP>:~/novel-backend/
scp Dockerfile docker-compose.yml .env.production reikaikurumi@<PUBLIC_IP>:~/novel-backend/
```

---

## 5. Cấu hình biến môi trường

```bash
# Trên VM
cd ~/novel-backend

# Copy template và chỉnh sửa
cp .env.production .env
nano .env
```

Điền các giá trị thực tế vào file `.env`:

```dotenv
# Database
DB_USERNAME=postgres
DB_PASSWORD=YOUR_STRONG_PASSWORD    # ← Đổi thành mật khẩu mạnh!
DB_NAME=noveldb

# Gemini AI
GEMINI_API_KEY=AIzaSy...           # ← API key của bạn

# JWT - Quan trọng: dùng key mạnh cho production!
JWT_SECRET=YOUR_PRODUCTION_JWT_SECRET_KEY

# Cloudinary
CLOUDINARY_CLOUD_NAME=dhhulbpvm
CLOUDINARY_API_KEY=923928446999419
CLOUDINARY_API_SECRET=p0yLwx1zh-...

# Mail (Resend)
MAIL_HOST=smtp.resend.com
MAIL_PORT=465
MAIL_USERNAME=resend
MAIL_PASSWORD=re_4rntCr51_...

# Frontend URL
APP_URL=http://<PUBLIC_IP>:3000
```

> 🔒 **Bảo mật**: Nhớ `chmod 600 .env` để chỉ owner mới đọc được.

```bash
chmod 600 .env
```

---

## 6. Build và chạy

```bash
cd ~/novel-backend

# Build và khởi chạy (lần đầu sẽ mất ~3-5 phút vì phải build Docker image)
docker compose up -d --build

# Xem logs realtime
docker compose logs -f

# Chỉ xem logs app
docker compose logs -f app

# Chỉ xem logs database
docker compose logs -f postgres
```

### Kiểm tra trạng thái:
```bash
# Xem container đang chạy
docker compose ps

# Kết quả mong đợi:
# NAME              IMAGE                     STATUS                    PORTS
# novel-backend     novel-backend-app         Up X minutes (healthy)    0.0.0.0:8080->8080/tcp
# pg_novel          pgvector/pgvector:pg18    Up X minutes (healthy)    0.0.0.0:5433->5432/tcp
```

---

## 7. Kiểm tra và quản lý

### Kiểm tra API hoạt động:
```bash
# Health check
curl http://localhost:8080/actuator/health

# Từ máy local (thay <PUBLIC_IP>)
curl http://<PUBLIC_IP>:8080/actuator/health
```

### Swagger UI:
Mở browser: `http://<PUBLIC_IP>:8080/swagger-ui.html`

Nếu dùng ngrok: `https://your-random-name.ngrok-free.dev/swagger-ui.html`

### Các lệnh quản lý thường dùng:
```bash
# Dừng tất cả
docker compose down

# Dừng và xóa cả volumes (⚠️ MẤT DỮ LIỆU DATABASE)
docker compose down -v

# Khởi động lại
docker compose restart

# Rebuild app (sau khi update code)
docker compose up -d --build app

# Xem logs 100 dòng cuối
docker compose logs --tail 100 app

# Vào container database
docker compose exec postgres psql -U postgres -d noveldb

# Xem resource usage
docker stats
```

### Cập nhật code (sau khi push code mới):
```bash
cd ~/novel-backend
git pull origin main
docker compose up -d --build app
```

---

## 8. Kết nối DBeaver từ máy local

> 💡 Dùng **SSH Tunnel** để kết nối an toàn — không cần mở port 5433 ra ngoài internet.

### Cách 1: Cấu hình SSH Tunnel trực tiếp trong DBeaver ✅ (Khuyến nghị)

**Bước 1**: Mở DBeaver → **Database** → **New Database Connection** → Chọn **PostgreSQL**

**Bước 2**: Tab **SSH** — tích chọn ✅ **Use SSH Tunnel**, điền thông tin:

| Trường | Giá trị |
|---|---|
| **Host/IP** | `<PUBLIC_IP>` |
| **Port** | `22` |
| **User name** | `reikaikurumi` |
| **Authentication** | Password hoặc Public Key |
| **Private key** | Đường dẫn tới file `.pem` (nếu dùng key) |

**Bước 3**: Tab **Main** — điền thông tin PostgreSQL:

| Trường | Giá trị |
|---|---|
| **Host** | `localhost` |
| **Port** | `5433` |
| **Database** | `noveldb` |
| **Username** | `postgres` |
| **Password** | Giá trị `DB_PASSWORD` trong file `.env` |

Nhấn **Test Connection** → **Finish**.

---

### Cách 2: SSH Tunnel thủ công (PowerShell)

```powershell
# Mở SSH tunnel — giữ cửa sổ này mở trong suốt quá trình dùng DBeaver
ssh -L 5433:localhost:5433 reikaikurumi@<PUBLIC_IP>
```

Sau đó kết nối DBeaver tới:
- **Host**: `localhost`
- **Port**: `5433`
- **Database**: `noveldb`
- **Username**: `postgres`
- **Password**: Giá trị `DB_PASSWORD` trong `.env`

---

### Truy cập nhanh qua psql (trên VM)

```bash
# Vào psql trực tiếp trong container
docker compose exec postgres psql -U postgres -d noveldb

# Các lệnh psql thường dùng
\dt          -- liệt kê tất cả bảng
\d ten_bang  -- xem cấu trúc bảng
\q           -- thoát

# Chạy query nhanh không cần vào interactive mode
docker compose exec postgres psql -U postgres -d noveldb -c "SELECT * FROM ten_bang LIMIT 10;"
```

---

## 9. HTTPS với ngrok (Tuỳ chọn)

> 💡 **Khi nào cần?** Nếu frontend deploy trên Vercel/Netlify (HTTPS) mà gọi API HTTP sẽ bị **Mixed Content** lỗi. Dùng ngrok để có HTTPS miễn phí với **URL cố định**, không cần domain, không cần cert.

### Cách hoạt động
```
Frontend (https://...) → https://your-name.ngrok-free.app → [ngrok] → http://localhost:8080
```

### Bước 1: Đăng ký tài khoản ngrok miễn phí

> 📝 Đăng ký tại: **https://dashboard.ngrok.com/signup**
> Hỗ trợ đăng nhập bằng **Google** hoặc **GitHub** → không cần tạo thêm password.

### Bước 2: Lấy authtoken

Sau khi đăng nhập vào **https://dashboard.ngrok.com/get-started/your-authtoken** → Copy authtoken.

### Bước 3: Cài đặt ngrok trên VM

```bash
# Tải và cài ngrok
curl -sSL https://ngrok-agent.s3.amazonaws.com/ngrok.asc \
  | sudo tee /etc/apt/trusted.gpg.d/ngrok.asc >/dev/null \
  && echo "deb https://ngrok-agent.s3.amazonaws.com buster main" \
  | sudo tee /etc/apt/sources.list.d/ngrok.list \
  && sudo apt update \
  && sudo apt install ngrok -y

# Kiểm tra
ngrok --version
```

### Bước 4: Cấu hình authtoken

```bash
ngrok config add-authtoken <YOUR_AUTHTOKEN>   # ← dán authtoken từ bước 2
```

### Bước 5: Lấy Static Domain miễn phí (URL cố định vĩnh viễn) ✅

> Free account được tặng **1 static domain duy nhất** không bao giờ đổi, **không thể tạo thêm**.

Vào **https://dashboard.ngrok.com/domains** → ngrok tự tạo domain dạng:
```
https://your-random-name.ngrok-free.dev   ← URL này cố định mãi mãi ✅
```

> ℹ️ **Lưu ý**: Suffix có thể là `.ngrok-free.dev` hoặc `.ngrok-free.app` tuỳ tài khoản. Copy chính xác domain từ dashboard, **không tự đổi suffix**.

### Bước 6: Chạy ngrok với static domain

> 📌 **Lưu ý suffix domain**: Copy **chính xác** domain từ dashboard (bước 5), **không tự đổi suffix** (`.dev` hay `.app`).

```bash
# Copy CHÍNH XÁC domain từ dashboard ngrok ở bước 5
nohup ngrok http --url=wondrously-aplanatic-lacresha.ngrok-free.dev 8080 > ~/ngrok.log 2>&1 &

# Kiểm tra ngrok đã chạy chưa (đợi vài giây)
# "nohup: ignoring input" là BÌNH THƯỜNG, không phải lỗi
sleep 3 && cat ~/ngrok.log

# Xem thông tin tunnel qua API (quan trọng: lấy public URL)
curl -s http://localhost:4040/api/tunnels | python3 -m json.tool
```

> ✅ Nếu thấy `"public_url": "https://wondrously-aplanatic-lacresha.ngrok-free.dev"` trong kết quả → ngrok đang hoạt động.

> ℹ️ Thông báo `nohup: ignoring input` khi chạy lệnh là **bình thường**, ngrok vẫn chạy ở background.

> ⚠️ Nếu `cat ~/ngrok.log` báo lỗi **ERR_NGROK_4018** → authtoken chưa được cấu hình đúng, chạy lại bước 4.

> ⚠️ Nếu báo lỗi **ERR_NGROK_302** → static domain không khớp với authtoken, kiểm tra lại domain ở bước 5.

> ⚠️ Nếu báo lỗi **ERR_NGROK_8012** hoặc "domain not found" → sai suffix (`.app` vs `.dev`), copy lại chính xác domain từ dashboard.

### ⚠️ Trang cảnh báo ngrok ("You are about to visit...")

Khi truy cập URL ngrok lần đầu trên **browser**, ngrok hiện trang cảnh báo này — chỉ cần nhấn **"Visit Site"** để tiếp tục. Trang này chỉ hiện **1 lần** cho mỗi browser session.

**Vấn đề**: Frontend gọi API (fetch/axios) sẽ bị chặn bởi trang cảnh báo này → API trả về HTML thay vì JSON.

**Fix phía frontend**: Thêm header `ngrok-skip-browser-warning: true` vào tất cả API request:

```javascript
// Ví dụ với axios
axios.defaults.headers.common['ngrok-skip-browser-warning'] = 'true';

// Hoặc với fetch
fetch(url, {
  headers: {
    'ngrok-skip-browser-warning': 'true'
  }
})
```

**Fix phía backend** (khuyến nghị — chỉ cần làm 1 lần): Thêm cấu hình vào `application.yml` để tự động thêm header response:

> Hoặc đơn giản nhất: **truy cập URL ngrok trên browser 1 lần** và nhấn "Visit Site" trước khi demo → browser session đã bypass, frontend sẽ hoạt động bình thường.

### Bước 7: Cập nhật APP_URL trong `.env`

```bash
nano ~/novel-backend/.env
```

```dotenv
# Đổi APP_URL thành URL ngrok static domain
APP_URL=https://your-random-name.ngrok-free.dev
```

Restart app:
```bash
cd ~/novel-backend && docker compose restart app
```

### Các lệnh ngrok thường dùng

```bash
# Xem tunnel đang chạy
curl http://localhost:4040/api/tunnels

# Dừng ngrok
pkill ngrok

# Chạy lại (URL vẫn giữ nguyên vì dùng static domain)
nohup ngrok http --url=your-random-name.ngrok-free.dev 8080 > ~/ngrok.log 2>&1 &
```

### Tự động chạy ngrok khi reboot VM

```bash
# Tạo systemd service
sudo nano /etc/systemd/system/ngrok.service
```

Nội dung file:
```ini
[Unit]
Description=ngrok tunnel
After=network.target

[Service]
User=reikaikurumi
ExecStart=/usr/bin/ngrok http --url=your-random-name.ngrok-free.dev 8080
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

```bash
# Kích hoạt service
sudo systemctl daemon-reload
sudo systemctl enable ngrok
sudo systemctl start ngrok
sudo systemctl status ngrok
```

### So sánh các lựa chọn

| | HTTP trực tiếp | ngrok (không static) | **ngrok Static Domain** ⭐ |
|---|---|---|---|
| Cần tài khoản | ❌ | ✅ | ✅ |
| URL cố định | IP cố định nhưng HTTP | ❌ Đổi mỗi lần | ✅ Cố định vĩnh viễn |
| HTTPS | ❌ | ✅ | ✅ |
| Miễn phí | ✅ | ✅ | ✅ |
| Phù hợp | Frontend HTTP | Demo 1 buổi | **Demo đồ án** |

---

## 10. Troubleshooting

### ❌ App không start / crash loop:
```bash
# Xem logs chi tiết
docker compose logs app

# Kiểm tra database đã ready chưa
docker compose logs postgres

# Kiểm tra .env file
cat .env
```

### ❌ Hết RAM (VM chỉ có 2GB):
```bash
# Kiểm tra memory
free -h
docker stats --no-stream

# Nếu hết RAM, thêm swap space:
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab
```

> 💡 **Khuyến nghị**: Với VM Standard_B1ms (2GB RAM), nên tạo swap 2GB ngay từ đầu.

### ❌ Port 8080 không truy cập được từ bên ngoài:
1. Kiểm tra NSG đã mở port 8080 chưa
2. Kiểm tra firewall trên VM:
```bash
sudo ufw status
sudo ufw allow 8080/tcp
```

### ❌ Database connection error:
```bash
# Kiểm tra postgres container
docker compose ps postgres
docker compose logs postgres

# Test kết nối từ app container
docker compose exec app sh -c "nc -zv postgres 5432"
```

### ❌ Build bị lỗi (hết disk):
```bash
# Kiểm tra disk
df -h

# Dọn Docker cache
docker system prune -a
```

---

## 📌 Tóm tắt nhanh (Quick Deploy)

```bash
# 1. SSH vào VM
ssh reikaikurumi@<PUBLIC_IP>

# 2. Cài Docker
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
sudo apt install docker-compose-plugin -y
newgrp docker

# 3. Tạo swap (quan trọng cho 2GB RAM)
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab

# 4. Clone code
git clone <REPO_URL> novel-backend
cd novel-backend

# 5. Cấu hình .env
cp .env.production .env
nano .env   # điền giá trị thực

# 6. Deploy!
docker compose up -d --build

# 7. Kiểm tra
docker compose ps
docker compose logs -f
curl http://localhost:8080/actuator/health
```

---

## 🎓 Lưu ý cho buổi bảo vệ đồ án

1. **Chuẩn bị trước buổi demo**: Deploy và test ít nhất 1 ngày trước
2. **Backup .env**: Lưu file .env ở nơi an toàn (không commit lên Git)
3. **Test API**: Dùng Postman collection có sẵn (`Novel-Backend-API.postman_collection.json`)
4. **Swagger**: Demo API docs tại `https://wondrously-aplanatic-lacresha.ngrok-free.dev/swagger-ui.html` (sau khi bật ngrok) hoặc `http://<PUBLIC_IP>:8080/swagger-ui.html` (HTTP trực tiếp)
5. **Nếu frontend dùng HTTPS**: Bật ngrok trước khi demo (xem [mục 8](#8-https-với-ngrok-tuỳ-chọn))
6. **Nếu demo bị lỗi**: Có thể nhanh chóng restart bằng `docker compose restart`
7. **Monitoring**: Mở terminal SSH sẵn để xem logs real-time khi demo

> 🍀 **Chúc bạn bảo vệ đồ án thành công!**

