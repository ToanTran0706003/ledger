// Biểu đồ SVG tự dựng (không thêm thư viện — taste-skill: kiểm soát thẩm mỹ, ít deps).
// AreaChart: số dư theo thời gian, có thể highlight một điểm (cho time-travel).

export function AreaChart({
  values,
  highlight,
  height = 140,
}: {
  values: number[];
  highlight?: number;
  height?: number;
}) {
  if (values.length === 0) {
    return <div className="muted" style={{ fontSize: 13 }}>Chưa có dữ liệu để vẽ.</div>;
  }

  const W = 600;
  const H = height;
  const pad = 8;
  const max = Math.max(...values, 1);
  const min = Math.min(...values, 0);
  const span = max - min || 1;
  const n = values.length;

  const xAt = (i: number) => (n === 1 ? W / 2 : pad + (i * (W - 2 * pad)) / (n - 1));
  const yAt = (v: number) => H - pad - ((v - min) / span) * (H - 2 * pad);

  const line = values.map((v, i) => `${i === 0 ? "M" : "L"}${xAt(i).toFixed(1)},${yAt(v).toFixed(1)}`).join(" ");
  const area = `${line} L${xAt(n - 1).toFixed(1)},${(H - pad).toFixed(1)} L${xAt(0).toFixed(1)},${(H - pad).toFixed(1)} Z`;

  const hi = highlight != null && highlight >= 0 && highlight < n ? highlight : null;

  return (
    <svg
      viewBox={`0 0 ${W} ${H}`}
      style={{ width: "100%", height: "auto", display: "block" }}
      role="img"
      aria-label="Biểu đồ số dư theo thời gian"
    >
      <path d={area} fill="var(--accent)" opacity="0.12" />
      <path d={line} fill="none" stroke="var(--accent)" strokeWidth="2" />
      {hi != null && (
        <>
          <line x1={xAt(hi)} y1={pad} x2={xAt(hi)} y2={H - pad} stroke="var(--faint)" strokeDasharray="3 4" />
          <circle cx={xAt(hi)} cy={yAt(values[hi])} r="4" fill="var(--accent)" stroke="var(--bg)" strokeWidth="2" />
        </>
      )}
    </svg>
  );
}
