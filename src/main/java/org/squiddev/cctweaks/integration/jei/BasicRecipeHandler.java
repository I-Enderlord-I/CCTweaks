package org.squiddev.cctweaks.integration.jei;

import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeHandler;
import mezz.jei.api.recipe.IRecipeWrapper;

import javax.annotation.Nonnull;

public class BasicRecipeHandler<T extends IValidRecipeWrapper> implements IRecipeHandler<T> {
	private final String id;
	private final Class<T> klass;

	public BasicRecipeHandler(IRecipeCategory category, Class<T> klass) {
		this.id = category.getUid();
		this.klass = klass;
	}

	@Nonnull
	@Override
	public Class<T> getRecipeClass() {
		return klass;
	}

	@Nonnull
	@Override
	public String getRecipeCategoryUid() {
		return id;
	}

	@Nonnull
	@Override
	public IRecipeWrapper getRecipeWrapper(@Nonnull T recipe) {
		return recipe;
	}

	@Override
	public boolean isRecipeValid(@Nonnull T recipe) {
		return recipe.isValid();
	}
}
