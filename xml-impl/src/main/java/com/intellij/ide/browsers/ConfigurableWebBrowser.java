package com.intellij.ide.browsers;

import java.util.UUID;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathUtil;
import com.intellij.xml.XmlIcons;

final class ConfigurableWebBrowser extends WebBrowser
{
	private final UUID id;
	@NotNull
	private BrowserFamily family;
	@NotNull
	private String name;
	private boolean active;
	private String path;

	private BrowserSpecificSettings specificSettings;

	@SuppressWarnings("UnusedDeclaration")
	public ConfigurableWebBrowser()
	{
		this(UUID.randomUUID(), BrowserFamily.CHROME);
	}

	public ConfigurableWebBrowser(@NotNull UUID id, @NotNull BrowserFamily family)
	{
		this(id, family, family.getName(), family.getExecutionPath(), true, family.createBrowserSpecificSettings());
	}

	public ConfigurableWebBrowser(@NotNull UUID id, @NotNull BrowserFamily family, @NotNull String name, @Nullable String path, boolean active,
			@Nullable BrowserSpecificSettings specificSettings)
	{
		this.id = id;
		this.family = family;
		this.name = name;

		this.path = StringUtil.nullize(path);
		this.active = active;
		this.specificSettings = specificSettings;
	}

	public void setName(@NotNull String value)
	{
		name = value;
	}

	public void setFamily(@NotNull BrowserFamily value)
	{
		family = value;
	}

	@NotNull
	@Override
	public Icon getIcon()
	{
		if(family == BrowserFamily.CHROME)
		{
			if(checkNameAndPath("Yandex"))
			{
				return XmlIcons.Browsers.Yandex16;
			}
			else if(checkNameAndPath("Dartium") || checkNameAndPath("Chromium"))
			{
				return XmlIcons.Browsers.Chromium16;
			}
			else if(checkNameAndPath("Canary"))
			{
				return XmlIcons.Browsers.Canary16;
			}
			else if(checkNameAndPath("Opera"))
			{
				return XmlIcons.Browsers.Opera16;
			}
			else if(checkNameAndPath("node-webkit") || checkNameAndPath("nw"))
			{
				return XmlIcons.Browsers.Node_webkit16;
			}
		}
		return family.getIcon();
	}

	private boolean checkNameAndPath(@NotNull String what)
	{
		if(StringUtil.containsIgnoreCase(name, what))
		{
			return true;
		}
		if(path != null)
		{
			int index = path.lastIndexOf('/');
			return index > 0 ? path.indexOf(what, index + 1) != -1 : path.contains(what);
		}
		return false;
	}

	@Nullable
	@Override
	public String getPath()
	{
		return path;
	}

	public void setPath(@Nullable String value)
	{
		path = PathUtil.toSystemIndependentName(StringUtil.nullize(value));
	}

	@Override
	@Nullable
	public BrowserSpecificSettings getSpecificSettings()
	{
		return specificSettings;
	}

	public void setSpecificSettings(@Nullable BrowserSpecificSettings value)
	{
		specificSettings = value;
	}

	public boolean isActive()
	{
		return active;
	}

	public void setActive(boolean value)
	{
		active = value;
	}

	@Override
	public boolean equals(Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof ConfigurableWebBrowser))
		{
			return false;
		}

		ConfigurableWebBrowser browser = (ConfigurableWebBrowser) o;
		return getId().equals(browser.getId()) &&
				family.equals(browser.family) &&
				active == browser.active &&
				Comparing.strEqual(name, browser.name) &&
				Comparing.equal(path, browser.path) &&
				Comparing.equal(specificSettings, browser.specificSettings);
	}

	@Override
	public int hashCode()
	{
		return getId().hashCode();
	}

	@Override
	@NotNull
	public String getName()
	{
		return name;
	}

	@Override
	@NotNull
	public final UUID getId()
	{
		return id;
	}

	@Override
	@NotNull
	public BrowserFamily getFamily()
	{
		return family;
	}

	@Override
	@NotNull
	public String getBrowserNotFoundMessage()
	{
		return IdeBundle.message("error.0.browser.path.not.specified", getName());
	}

	@Override
	public String toString()
	{
		return getName() + " (" + getPath() + ")";
	}
}