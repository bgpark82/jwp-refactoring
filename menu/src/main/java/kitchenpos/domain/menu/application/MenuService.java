package kitchenpos.domain.menu.application;

import kitchenpos.domain.menu.domain.Menu;
import kitchenpos.domain.menu.domain.MenuRepository;
import kitchenpos.domain.menu.dto.MenuExistRequest;
import kitchenpos.domain.menu.dto.MenuRequest;
import kitchenpos.exception.BusinessException;
import kitchenpos.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuValidator menuValidator;

    public MenuService(
            final MenuRepository menuRepository,
            final MenuValidator menuValidator
    ) {
        this.menuRepository = menuRepository;
        this.menuValidator = menuValidator;
    }

    @Transactional
    public Menu create(final MenuRequest request) {
        final Menu menu = request.toMenu();
        menuValidator.validatePrice(menu);
        return menuRepository.save(menu);
    }

    public List<Menu> list() {
        return menuRepository.findAll();
    }

    public void validateMenuExist(MenuExistRequest request) {
        if (request.getMenuIds().size() != menuRepository.countByIdIn(request.getMenuIds())) {
            throw new BusinessException(ErrorCode.MENU_NOT_EXIST);
        }
    }
}
