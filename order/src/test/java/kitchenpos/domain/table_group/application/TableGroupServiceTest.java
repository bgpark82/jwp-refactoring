package kitchenpos.domain.table_group.application;

import kitchenpos.domain.order.domain.InMemoryOrderRepository;
import kitchenpos.domain.order.domain.OrderRepository;
import kitchenpos.domain.order.domain.OrderStatus;
import kitchenpos.domain.table.domain.InMemoryOrderTableRepository;
import kitchenpos.domain.table.domain.OrderTable;
import kitchenpos.domain.table.domain.OrderTableRepository;
import kitchenpos.domain.table_group.domain.InMemoryTableGroupRepository;
import kitchenpos.domain.table_group.domain.TableGroup;
import kitchenpos.domain.table_group.domain.TableGroupRepository;
import kitchenpos.domain.table_group.dto.TableGroupRequest;
import kitchenpos.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.List;

import static kitchenpos.domain.order.fixture.OrderFixture.주문;
import static kitchenpos.domain.table.fixture.OrderTableFixture.주문_테이블;
import static kitchenpos.domain.table_group.fixture.TableGroupFixture.단체_지정_요청;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * - 단체 지정을 저장할 수 있다
 * - 단체 지정의 주문 테이블의 개수가 올바르지 않으면 단체 지정을 저장할 수 없다
 *     - 주문 테이블 목록이 비어 있을 수 없다
 *     - 주문 테이블 목록의 크기는 2 이상이어야 한다
 * - 단체 지정의 저장된 주문 테이블이 올바르지 않으면 단체 지정을 저장할 수 없다
 *     - 주문 테이블 목록과 저장된 저장된 주문 테이블 목록의 크기가 같아야 한다
 * - 단체 지정의 주문 테이블이 올바르지 않으면 단체 지정을 저장할 수 없다
 *     - 주문 테이블 목록이 비어 있을 수 없다
 *     - 주문 테이블의 테이블 그룹 아이디는 존재해야 한다
 * - 단체 지정을 해제할 수 있다
 * - 단체 지정의 주문 테이블 아이디와 주문 혹은 식사 주문 상태인 주문이 존재하면 단체 지정을 해제할 수 없다
 */
class TableGroupServiceTest {

    private OrderRepository orderRepository;
    private OrderTableRepository orderTableRepository;
    private TableGroupRepository tableGroupRepository;
    private TableGroupService tableGroupService;
    private TableGroupValidator tableGroupValidator;

    @BeforeEach
    void setUp() {
        orderRepository = new InMemoryOrderRepository();
        orderTableRepository = new InMemoryOrderTableRepository();
        tableGroupRepository = new InMemoryTableGroupRepository();
        tableGroupValidator = new TableGroupValidator(orderRepository, orderTableRepository);
        tableGroupService = new TableGroupService(tableGroupRepository, tableGroupValidator);
    }

    @Test
    void create_단체_지정을_저장할_수_있다() {
        OrderTable 저장된_주문_테이블1 = orderTableRepository.save(빈_주문_테이블());
        OrderTable 저장된_주문_테이블2 = orderTableRepository.save(빈_주문_테이블());
        TableGroupRequest 단체_지정 = 단체_지정_요청(Arrays.asList(저장된_주문_테이블1, 저장된_주문_테이블2));

        TableGroup 저장된_단체_지정 = tableGroupService.create(단체_지정);

        List<OrderTable> 저장된_주문_테이블 = orderTableRepository.findAll();
        assertAll(
                () -> assertThat(저장된_주문_테이블.size()).isEqualTo(2),
                () -> assertThat(저장된_주문_테이블.get(0).isEmpty()).isFalse(),
                () -> assertThat(저장된_주문_테이블.get(0).getTableGroupId()).isEqualTo(저장된_단체_지정.getId()),
                () -> assertThat(저장된_단체_지정.getCreatedDate()).isNotNull()
        );
    }

    @Test
    void create_단체_지정의_주문_테이블의_개수가_올바르지_않으면_단체_지정을_저장할_수_없다() {
        OrderTable 저장된_주문_테이블1 = orderTableRepository.save(빈_주문_테이블());
        TableGroupRequest tableGroup = 단체_지정_요청(Arrays.asList(저장된_주문_테이블1));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> tableGroupService.create(tableGroup));
    }

    @Test
    void create_단체_지정의_저장된_주문_테이블이_올바르지_않으면_단체_지정을_저장할_수_없다() {
        OrderTable 저장된_주문_테이블 = orderTableRepository.save(빈_주문_테이블());
        TableGroupRequest tableGroup = 단체_지정_요청(Arrays.asList(저장된_주문_테이블, 빈_주문_테이블()));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> tableGroupService.create(tableGroup));
    }

    @Test
    void create_단체_지정의_주문_테이블이_올바르지_않으면_단체_지정을_저장할_수_없다() {
        OrderTable 빈_주문_테이블 = orderTableRepository.save(주문_테이블(0, 1L, true));
        OrderTable 채워진_주문_테이블 = orderTableRepository.save(주문_테이블(2, null, false));
        TableGroupRequest tableGroup = 단체_지정_요청(Arrays.asList(빈_주문_테이블, 채워진_주문_테이블));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> tableGroupService.create(tableGroup));
    }

    @Test
    void ungroup_단체_지정을_해제할_수_있다() {
        OrderTable 저장된_주문_테이블1 = orderTableRepository.save(빈_주문_테이블());
        OrderTable 저장된_주문_테이블2 = orderTableRepository.save(빈_주문_테이블());
        TableGroupRequest 단체_지정 = 단체_지정_요청(Arrays.asList(저장된_주문_테이블1, 저장된_주문_테이블2));
        TableGroup 저장된_단체_지정 = tableGroupService.create(단체_지정);

        tableGroupService.ungroup(저장된_단체_지정.getId());

        List<OrderTable> 저장된_주문_테이블 = orderTableRepository.findAll();
        저장된_주문_테이블
                .forEach(orderTable -> assertThat(orderTable.getTableGroupId()).isNull());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"COOKING","MEAL"})
    void ungroup_단체_지정의_주문_테이블_아이디와_주문_혹은_식사_주문_상태인_주문이_존재하면_단체_지정을_해제할_수_없다(OrderStatus 유효하지_않은_주문_상태) {
        OrderTable 저장된_주문_테이블1 = orderTableRepository.save(빈_주문_테이블());
        OrderTable 저장된_주문_테이블2 = orderTableRepository.save(빈_주문_테이블());
        orderRepository.save(주문(저장된_주문_테이블1, Arrays.asList(), 유효하지_않은_주문_상태));
        TableGroupRequest 단체_지정 = 단체_지정_요청(Arrays.asList(저장된_주문_테이블1, 저장된_주문_테이블2));
        TableGroup 저장된_단체_지정 = tableGroupService.create(단체_지정);

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> tableGroupService.ungroup(저장된_단체_지정.getId()));
    }

    private OrderTable 빈_주문_테이블() {
        return 주문_테이블(0, true);
    }
}
