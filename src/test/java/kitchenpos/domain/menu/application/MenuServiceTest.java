package kitchenpos.domain.menu.application;

import kitchenpos.domain.menu.domain.InMemoryMenuRepository;
import kitchenpos.domain.menu_group.domain.InMemoryMenuGroupRepository;
import kitchenpos.domain.menu.domain.Menu;
import kitchenpos.domain.menu.domain.MenuRepository;
import kitchenpos.domain.menu_group.domain.MenuGroup;
import kitchenpos.domain.menu_group.domain.MenuGroupRepository;
import kitchenpos.domain.product.domain.InMemoryProductRepository;
import kitchenpos.domain.product.domain.Product;
import kitchenpos.domain.product.domain.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;

import static kitchenpos.domain.menu.fixture.MenuFixture.메뉴_요청;
import static kitchenpos.domain.menu_group.fixture.MenuGroupFixture.메뉴_그룹;
import static kitchenpos.domain.menu.fixture.MenuProductFixture.메뉴_상품_요청;
import static kitchenpos.domain.product.fixture.ProductFixture.상품;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * - 메뉴를 등록할 수 있다
 * - 메뉴의 가격이 올바르지 않으면 등록할 수 없다.
 *     - 메뉴의 가격은 존재해야 한다
 *     - 메뉴의 가격은 0원 이상이어야한다.
 * - 메뉴의 메뉴 그룹이 존재하지 않으면 등록할 수 없다.
 * - 메뉴 상품의 상품이 존재하지 않으면 등록할 수 없다
 * - 메뉴 상품의 가격은 상품의 가격과 메뉴 상품의 가격의 곱이다
 * - 메뉴의 가격이 올바르지 않으면 등록 할 수 없다
 *     - 메뉴의 가격은 메뉴 상품의 가격의 합보다 작아야 한다.
 * - 메뉴 목록을 조회할 수 있다
 */
class MenuServiceTest {

    private static final int 수량 = 2;
    private static final String 메뉴_이름 = "후라이드+후라이드";
    private static final BigDecimal 메뉴_가격 = new BigDecimal(19_000);
    private static final MenuGroup 메뉴_그룹 = 메뉴_그룹();
    private static final Product 상품 = 상품("강정치킨", new BigDecimal(17_000));

    private MenuRepository menuRepository;
    private MenuGroupRepository menuGroupRepository;
    private ProductRepository productRepository;
    private MenuValidator menuValidator;
    private MenuService menuService;
    private MenuGroup 저장된_메뉴_그룹;
    private Product 저장된_상품;

    @BeforeEach
    void setUp() {
        menuRepository = new InMemoryMenuRepository();
        menuGroupRepository = new InMemoryMenuGroupRepository();
        productRepository = new InMemoryProductRepository();
        menuValidator = new MenuValidator(menuGroupRepository, productRepository);
        menuService = new MenuService(menuRepository, menuValidator);

        저장된_메뉴_그룹 = menuGroupRepository.save(메뉴_그룹);
        저장된_상품 = productRepository.save(상품);
    }

    @Test
    void create_메뉴를_등록할_수_있다() {
        Menu 저장된_메뉴 = menuService.create(메뉴_요청(저장된_메뉴_그룹, 메뉴_이름, 메뉴_가격, 메뉴_상품_요청(저장된_상품, 수량)));
        assertAll(
                () -> assertThat(저장된_메뉴.getPrice()).isEqualTo(메뉴_가격),
                () -> assertThat(저장된_메뉴.getName()).isEqualTo(메뉴_이름),
                () -> assertThat(저장된_메뉴.getMenuGroupId()).isEqualTo(저장된_메뉴_그룹.getId()),
                () -> assertThat(저장된_메뉴.getMenuProducts().size()).isEqualTo(1),
                () -> assertThat(저장된_메뉴.getMenuProducts().get(0).getProductId()).isEqualTo(저장된_상품.getId()),
                () -> assertThat(저장된_메뉴.getMenuProducts().get(0).getQuantity()).isEqualTo(수량)
        );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"-1"})
    void create_메뉴의_가격이_올바르지_않으면_등록할_수_없다(BigDecimal 올바르지_않은_가격) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> menuService.create(메뉴_요청(저장된_메뉴_그룹, 메뉴_이름, 올바르지_않은_가격, 메뉴_상품_요청(저장된_상품, 수량))));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L})
    void create_메뉴의_메뉴_그룹이_존재하지_않으면_등록할_수_없다(Long 존재하지_않는_메뉴_그룹_아이디) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> menuService.create(메뉴_요청(존재하지_않는_메뉴_그룹_아이디, 메뉴_이름, 메뉴_가격, 메뉴_상품_요청(저장된_상품, 수량))));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L})
    void create_메뉴_상품의_상품이_존재하지_않으면_등록할_수_없다(Long 존재하지_않는_상품_아이디) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> menuService.create(메뉴_요청(저장된_메뉴_그룹, 메뉴_이름, 메뉴_가격, 메뉴_상품_요청(존재하지_않는_상품_아이디, 수량))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"100000"})
    void create_메뉴의_가격이_올바르지_않으면_등록_할_수_없다(BigDecimal 올바르지_않은_메뉴_가격) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> menuService.create(메뉴_요청(저장된_메뉴_그룹, 메뉴_이름, 올바르지_않은_메뉴_가격, 메뉴_상품_요청(저장된_상품, 수량))));
    }

    @Test
    void list_메뉴_목록을_조회할_수_있다() {
        menuService.create(메뉴_요청(저장된_메뉴_그룹, 메뉴_이름, 메뉴_가격, 메뉴_상품_요청(저장된_상품, 수량)));
        List<Menu> menus = menuService.list();
        assertAll(
                () -> assertThat(menus.size()).isEqualTo(1),
                () -> assertThat(menus.get(0).getName()).isEqualTo(메뉴_이름),
                () -> assertThat(menus.get(0).getPrice()).isEqualTo(메뉴_가격),
                () -> assertThat(menus.get(0).getMenuGroupId()).isEqualTo(저장된_메뉴_그룹.getId()),
                () -> assertThat(menus.get(0).getMenuProducts().size()).isEqualTo(1),
                () -> assertThat(menus.get(0).getMenuProducts().get(0).getProductId()).isEqualTo(저장된_상품.getId()),
                () -> assertThat(menus.get(0).getMenuProducts().get(0).getQuantity()).isEqualTo(수량)
        );
    }
}
