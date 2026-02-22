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
8. [Troubleshooting](#8-troubleshooting)

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

## 8. Troubleshooting

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
4. **Swagger**: Demo API docs tại `http://<PUBLIC_IP>:8080/swagger-ui.html`
5. **Nếu demo bị lỗi**: Có thể nhanh chóng restart bằng `docker compose restart`
6. **Monitoring**: Mở terminal SSH sẵn để xem logs real-time khi demo

> 🍀 **Chúc bạn bảo vệ đồ án thành công!**

