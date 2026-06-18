import type { ReactNode } from "react";

export type Notify = (msg: string, kind?: "ok" | "err") => void;

export function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  return (
    <div className="modal-backdrop" onClick={onClose} role="dialog" aria-modal="true" aria-label={title}>
      <div className="modal card" onClick={(e) => e.stopPropagation()}>
        <div className="spread" style={{ marginBottom: 14 }}>
          <h2>{title}</h2>
          <button className="ghost" onClick={onClose} aria-label="Đóng">✕</button>
        </div>
        {children}
      </div>
    </div>
  );
}
