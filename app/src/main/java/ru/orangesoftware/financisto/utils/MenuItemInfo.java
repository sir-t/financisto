/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.utils;

public class MenuItemInfo {
	public final int menuId;
	public int titleId;
	public int iconId;
	public boolean enabled = true;
	
	public MenuItemInfo(int menuId, int titleId) {
		this(menuId, titleId, 0);
	}

	public MenuItemInfo(int menuId, int titleId, int iconId) {
		this.menuId = menuId;
		this.titleId = titleId;
		this.iconId = iconId;
	}

}
