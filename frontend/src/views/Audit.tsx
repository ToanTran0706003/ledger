import { useEffect, useState } from "react";
import { api, ApiError } from "../api";
import type { IntegrityReport } from "../api";
import { money } from "../format";
import type { Notify } from "../ui";

export function Audit({ notify }: { notify: Notify }) {
  const [report, setReport] = useState<IntegrityReport | null>(null);

  useEffect(() => {
    api
      .integrity()
      .then(setReport)
      .catch((ex) => notify(ex instanceof ApiError ? ex.message : "Không tải được báo cáo.", "err"));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const diff = report ? report.totalBalance - report.expectedTotal : 0;

  return (
    <div className="stack" style={{ maxWidth: 680, marginInline: "auto", width: "100%" }}>
      <div>
        <div className="eyebrow">Kiểm toán</div>
        <h1>Toàn vẹn sổ cái</h1>
      </div>
      <p className="muted">
        Nhờ ghi sổ kép, tổng số dư mọi tài khoản (kể cả két hệ thống) luôn bằng lượng tiền phát hành ban đầu. Lệch dù
        một đồng nghĩa là có lỗi nghiêm trọng. Mỗi giao dịch cân vế nên tổng không bao giờ đổi.
      </p>
      {!report ? (
        <div className="skeleton" style={{ height: 180 }} />
      ) : (
        <div className="card stack">
          <span className={"badge " + (report.balanced ? "ok" : "err")} style={{ alignSelf: "flex-start" }}>
            <span className="dot" />
            {report.balanced ? "Sổ cân" : "Lệch sổ"}
          </span>
          <div className="stat-grid">
            <div className="stat" style={{ padding: 0 }}>
              <div className="label">Tổng số dư</div>
              <div className="value">{money(report.totalBalance)}</div>
            </div>
            <div className="stat" style={{ padding: 0 }}>
              <div className="label">Lượng phát hành</div>
              <div className="value">{money(report.expectedTotal)}</div>
            </div>
            <div className="stat" style={{ padding: 0 }}>
              <div className="label">Chênh lệch</div>
              <div className="value">{money(diff)}</div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
