// Định dạng tiền và thời gian theo locale Việt Nam. Số tiền hiển thị rõ ràng (tin cậy).
export function money(amount: number, currency = "VND"): string {
  return `${new Intl.NumberFormat("vi-VN").format(amount)} ${currency}`;
}

export function dateTime(iso: string): string {
  return new Date(iso).toLocaleString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function shortId(id: string): string {
  return id.length > 12 ? `${id.slice(0, 8)}…` : id;
}

const MOVEMENT_LABELS: Record<string, string> = {
  DEPOSIT: "Nạp tiền",
  WITHDRAWAL: "Rút tiền",
  TRANSFER: "Chuyển tiền",
  GENESIS: "Khởi tạo",
  REVERSAL: "Bù trừ",
  INTEREST: "Lãi",
  CAPTURE: "Thu giữ chỗ",
};
export function movementLabel(type: string): string {
  return MOVEMENT_LABELS[type] ?? type;
}

const HOLD_STATUS_LABELS: Record<string, string> = {
  ACTIVE: "Đang giữ",
  RELEASED: "Đã nhả",
  CAPTURED: "Đã thu",
};
export function holdStatusLabel(status: string): string {
  return HOLD_STATUS_LABELS[status] ?? status;
}

export function accountTypeLabel(type: string): string {
  if (type === "SAVINGS") return "Tiết kiệm";
  if (type === "SYSTEM_VAULT") return "Két hệ thống";
  return "Thanh toán";
}

export function intervalLabel(seconds: number): string {
  if (seconds % 86400 === 0) return `mỗi ${seconds / 86400} ngày`;
  if (seconds % 3600 === 0) return `mỗi ${seconds / 3600} giờ`;
  if (seconds % 60 === 0) return `mỗi ${seconds / 60} phút`;
  return `mỗi ${seconds} giây`;
}
