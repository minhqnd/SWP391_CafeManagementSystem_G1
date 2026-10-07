#!/bin/bash
set -e

echo "=========================================================="
echo "    AURA CAFE ERP - AUTOMATED SERVER DEPLOYMENT          "
echo "=========================================================="

# 1. Kiểm tra và cài đặt Docker nếu chưa có
if ! command -v docker &> /dev/null; then
    echo ">> Docker chưa được cài đặt. Đang tiến hành cài đặt Docker tự động..."
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    rm -f get-docker.sh
    sudo usermod -aG docker "$USER" || true
    sudo systemctl enable --now docker
    echo ">> Cài đặt Docker thành công!"
else
    echo ">> Docker đã sẵn sàng."
fi

# 2. Khởi động Docker Service (nếu chưa chạy)
sudo systemctl start docker || true

# 3. Pull image mới và build/chạy hệ thống qua Docker Compose
echo ">> Đang build và khởi động hệ thống (MySQL + Tomcat 10.1)..."
sudo docker compose down 2>/dev/null || true
sudo docker compose up -d --build

echo "=========================================================="
echo ">> Kiểm tra trạng thái các container:"
sudo docker compose ps

SERVER_IP=$(curl -s ifconfig.me || echo "136.118.31.255")

echo "=========================================================="
echo " TRIỂN KHAI THÀNH CÔNG!"
echo " Hệ thống Cafe Management System đang chạy tại:"
echo " -> http://${SERVER_IP}:8080/SWP391_CafeManagementSystem/"
echo " -> Hoặc: http://${SERVER_IP}:8080/ (tự động chuyển hướng)"
echo ""
echo " Để xem log hoạt động:"
echo "   sudo docker compose logs -f"
echo "=========================================================="
