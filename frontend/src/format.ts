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
};
export function movementLabel(type: string): string {
  return MOVEMENT_LABELS[type] ?? type;
}
