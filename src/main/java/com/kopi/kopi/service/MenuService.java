package com.kopi.kopi.service;

import com.kopi.kopi.controller.MenuController;
import java.util.List;

public interface MenuService {
    List<MenuController.MenuCategoryDto> getMenu();
}

