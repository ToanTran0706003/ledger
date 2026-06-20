import { useEffect, useState } from "react";
import { api, ApiError } from "../api";
import type { FrozenAccount, HashChainReport } from "../api";
import { shortId } from "../format";
import type { Notify } from "../ui";

/**
 * Bảng vận hành & kiểm toán (ADMIN/AUDITOR). Hai năng lực vốn chỉ có ở API nay được surface:
 * xác minh hash-chain (sổ cái không bị giả mạo) và xử lý các tài khoản bị đóng băng.
 */
export function Admin({ notify, roles }: { notify: Notify; roles: string[] }) {
  const isAdmin = roles.includes("ADMIN");
  const [chain, setChain] = useState<HashChainReport | null>(null);
  const [verifying, setVerifying] = useState(false);
  const [frozen, setFrozen] = useState<FrozenAccount[] | null>(null);

  async function loadFrozen() {
    try {
      setFrozen(await api.frozenAccounts());
    } catch (ex) {
      notify(ex instanceof ApiError ? ex.message : "Không tải được danh sách đóng băng.", "err");
    }
  }

  useEffect(() => {
    if (isAdmin) loadFrozen();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function verify() {
    setVerifying(true);
    try {
      setChain(await api.hashChain());
    } catch (ex) {
      notify(ex instanceof ApiError ? ex.message : "Không kiểm tra được hash-chain.", "err");
    } finally {
      setVerifying(false);
    }
  }

  async function unfreeze(id: string) {
    try {
      await api.unfreezeAccount(id);
      notify("Đã mở băng tài khoản.");
      await loadFrozen();
    } catch (ex) {
      notify(ex instanceof ApiError ? ex.message : "Không mở băng được.", "err");
    }
  }

  return (
    <div className="stack" style={{ maxWidth: 720 }}>
      <div>
        <div className="eyebrow">Vận hành & kiểm toán</div>
        <h1>Quản trị</h1>
      </div>

      <div className="card stack">
        <div className="spread">
          <div>
            <h2>Toàn vẹn chuỗi sự kiện</h2>
            <p className="muted" style={{ margin: 0 }}>
              Hash-chain phát hiện sửa-tại-chỗ hoặc xoá event giữa chuỗi trên sổ cái.
            </p>
          </div>
          <button className="primary" onClick={verify} disabled={verifying}>
            {verifying ? "Đang kiểm" : "Kiểm tra hash-chain"}
          </button>
        </div>
        {chain && (
          <div className="spread">
            <span className={"badge " + (chain.intact ? "ok" : "err")}>
              <span className="dot" />
              {chain.intact ? "Nguyên vẹn" : "Phát hiện sửa đổi"}
            </span>
            <span className="faint num">
              {chain.eventsChecked} event đã kiểm
              {chain.intact ? "" : ` · gãy tại #${chain.firstBrokenSeq}`}
            </span>
          </div>
        )}
      </div>

      {isAdmin && (
        <div className="card">
          <h2>Tài khoản bị đóng băng</h2>
          {frozen === null ? (
            <div className="skeleton" style={{ height: 60 }} />
          ) : frozen.length === 0 ? (
            <p className="muted">Không có tài khoản nào bị đóng băng.</p>
          ) : (
            <div className="feed">
              {frozen.map((f) => (
                <div key={f.accountId} className="feed-row">
                  <div className="meta">
                    <span>
                      <span className="chip-frozen">❄ {shortId(f.accountId)}</span>{" "}
                      <span className="faint">chủ {shortId(f.owner)}</span>
                    </span>
                    <span className="when">{f.freezeReason}</span>
                  </div>
                  <button className="ghost" onClick={() => unfreeze(f.accountId)}>
                    Mở băng
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
