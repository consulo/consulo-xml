// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.xml.actions.validate;

import consulo.xml.psi.xml.XmlFile;
import consulo.component.extension.ExtensionPointName;

public interface ValidateXmlHandler
{
	ExtensionPointName<ValidateXmlHandler> EP_NAME = ExtensionPointName.create("com.intellij.xml.validateHandler");

	void doValidate(XmlFile file);

	boolean isAvailable(XmlFile file);
}
