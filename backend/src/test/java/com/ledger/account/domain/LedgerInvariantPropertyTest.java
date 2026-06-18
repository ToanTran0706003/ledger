package com.ledger.account.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property-based test (jqwik): với MỌI dãy giao dịch ngẫu nhiên (nạp/rút/chuyển,
 * hợp lệ lẫn không), sau từng bước các invariant tài chính luôn đúng. Nếu một dãy
 * phá vỡ invariant, jqwik tự thu nhỏ về ca lỗi tối thiểu.
 *
 * Test ở mức domain thuần (không DB) — nơi invariant thực sự được enforce.
 */
class LedgerInvariantPropertyTest {

    private static final BigDecimal SEED = new BigDecimal("1000000");
    private static final int CUSTOMER_COUNT = 3;

    enum OpType {
        DEPOSIT,
        WITHDRAW,
        TRANSFER
    }

    record Op(OpType type, int fromIdx, int toIdx, BigDecimal amount) {}

    @Property(tries = 300)
    void invariants_hold_after_every_operation(@ForAll("operations") List<Op> ops) {
        AccountAggregate vault = open(SystemAccounts.VAULT_ID, AccountType.SYSTEM_VAULT);
        vault.credit("genesis", SEED, MovementType.GENESIS, null);

        List<AccountAggregate> customers = new ArrayList<>();
        for (int i = 0; i < CUSTOMER_COUNT; i++) {
            customers.add(open("cust-" + i, AccountType.CUSTOMER));
        }

        for (Op op : ops) {
            switch (op.type()) {
                case DEPOSIT -> move(vault, customers.get(op.toIdx()), op.amount());
                case WITHDRAW -> move(customers.get(op.fromIdx()), vault, op.amount());
                case TRANSFER -> {
                    if (op.fromIdx() != op.toIdx()) {
                        move(customers.get(op.fromIdx()), customers.get(op.toIdx()), op.amount());
                    }
                }
            }

            // Invariant 1: không tài khoản CUSTOMER nào âm.
            for (AccountAggregate c : customers) {
                assertThat(c.balance().signum()).isGreaterThanOrEqualTo(0);
            }
            // Invariant 2: tổng số dư toàn hệ thống == hằng số khởi tạo.
            assertThat(total(vault, customers)).isEqualByComparingTo(SEED);
        }
    }

    /** Di chuyển tiền: debit nguồn (có thể bị từ chối), nếu OK thì credit đích. Cân vế. */
    private void move(AccountAggregate from, AccountAggregate to, BigDecimal amount) {
        try {
            from.debit("tx", amount, MovementType.TRANSFER, to.accountId());
        } catch (InsufficientFundsException rejected) {
            return; // bị từ chối -> không có hiệu lực, đúng đắn
        }
        to.credit("tx", amount, MovementType.TRANSFER, from.accountId());
    }

    private static BigDecimal total(AccountAggregate vault, List<AccountAggregate> customers) {
        BigDecimal sum = vault.balance();
        for (AccountAggregate c : customers) {
            sum = sum.add(c.balance());
        }
        return sum;
    }

    private static AccountAggregate open(String id, AccountType type) {
        AccountAggregate account = new AccountAggregate();
        account.open(id, "owner-" + id, type);
        return account;
    }

    @Provide
    Arbitrary<List<Op>> operations() {
        Arbitrary<Op> op = Combinators.combine(
                        Arbitraries.of(OpType.values()),
                        Arbitraries.integers().between(0, CUSTOMER_COUNT - 1),
                        Arbitraries.integers().between(0, CUSTOMER_COUNT - 1),
                        Arbitraries.longs().between(1, 10_000))
                .as((type, from, to, amount) -> new Op(type, from, to, BigDecimal.valueOf(amount)));
        return op.list().ofMaxSize(200);
    }
}
