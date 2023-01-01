/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.xml.util.xml.highlighting;

import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.codeInsight.daemon.impl.TrafficLightRenderer;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.rawHighlight.SeverityRegistrar;
import consulo.language.psi.PsiDocumentManager;
import consulo.project.Project;
import consulo.ui.ex.awt.util.Alarm;
import consulo.util.collection.ContainerUtil;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.util.xml.DomChangeAdapter;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomManager;
import consulo.xml.util.xml.DomUtil;
import consulo.xml.util.xml.ui.CommittablePanel;
import consulo.xml.util.xml.ui.Highlightable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

/**
 * User: Sergey.Vasiliev
 */
public class DomElementsErrorPanel extends JPanel implements CommittablePanel, Highlightable
{
	private static final int ALARM_PERIOD = 241;

	private final Project myProject;
	private final DomElement[] myDomElements;

	private final DomElementsTrafficLightRenderer myErrorStripeRenderer;
	private final DomElementAnnotationsManagerImpl myAnnotationsManager;

	private final Alarm myAlarm = new Alarm();

	public DomElementsErrorPanel(final DomElement... domElements)
	{
		assert domElements.length > 0;

		myDomElements = domElements;
		final DomManager domManager = domElements[0].getManager();
		myProject = domManager.getProject();
		myAnnotationsManager = (DomElementAnnotationsManagerImpl) DomElementAnnotationsManager.getInstance(myProject);

		setPreferredSize(getDimension());

		myErrorStripeRenderer = new DomElementsTrafficLightRenderer(DomUtil.getFile(domElements[0]));
		Disposer.register(this, myErrorStripeRenderer);

		addUpdateRequest();
		domManager.addDomEventListener(new DomChangeAdapter()
		{
			@Override
			protected void elementChanged(DomElement element)
			{
				addUpdateRequest();
			}
		}, this);
	}

	@Override
	public void updateHighlighting()
	{
		updatePanel();
	}

	private boolean areValid()
	{
		for(final DomElement domElement : myDomElements)
		{
			if(!domElement.isValid())
			{
				return false;
			}
		}
		return true;
	}

	private void updatePanel()
	{
		myAlarm.cancelAllRequests();

		if(!areValid())
		{
			return;
		}

		repaint();
		//setToolTipText(myErrorStripeRenderer.getTooltipMessage());

		if(!isHighlightingFinished())
		{
			addUpdateRequest();
		}
	}

	private boolean isHighlightingFinished()
	{
		return !areValid() || myAnnotationsManager.isHighlightingFinished(myDomElements);
	}

	private void addUpdateRequest()
	{
		ApplicationManager.getApplication().invokeLater(new Runnable()
		{

			@Override
			public void run()
			{
				myAlarm.addRequest(new Runnable()
				{
					@Override
					public void run()
					{
						if(myProject.isOpen() && !myProject.isDisposed())
						{
							updatePanel();
						}
					}
				}, ALARM_PERIOD);
			}
		});
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		myErrorStripeRenderer.paint(this, g, new Rectangle(0, 0, getWidth(), getHeight()));
	}

	@Override
	public void dispose()
	{
		myAlarm.cancelAllRequests();
	}

	@Override
	public JComponent getComponent()
	{
		return this;
	}

	@Override
	public void commit()
	{
	}

	@Override
	public void reset()
	{
		updatePanel();
	}

	private static Dimension getDimension()
	{
		return new Dimension(AllIcons.General.ErrorsInProgress.getWidth() + 2, AllIcons.General.ErrorsInProgress.getHeight() + 2);
	}

	private class DomElementsTrafficLightRenderer extends TrafficLightRenderer
	{
		public DomElementsTrafficLightRenderer(@Nonnull XmlFile xmlFile)
		{
			super(xmlFile.getProject(), PsiDocumentManager.getInstance(xmlFile.getProject()).getDocument(xmlFile));
		}

		@Nonnull
		@Override
		protected DaemonCodeAnalyzerStatus getDaemonCodeAnalyzerStatus(@Nonnull SeverityRegistrar severityRegistrar)
		{
			final DaemonCodeAnalyzerStatus status = super.getDaemonCodeAnalyzerStatus(severityRegistrar);
			if(isInspectionCompleted())
			{
				status.errorAnalyzingFinished = true;
			}
			return status;
		}

		@Override
		protected void fillDaemonCodeAnalyzerErrorsStatus(@Nonnull DaemonCodeAnalyzerStatus status, @Nonnull SeverityRegistrar severityRegistrar)
		{
			for(int i = 0; i < status.errorCount.length; i++)
			{
				final HighlightSeverity minSeverity = severityRegistrar.getSeverityByIndex(i);
				if(minSeverity == null)
				{
					continue;
				}

				int sum = 0;
				for(DomElement element : myDomElements)
				{
					final DomElementsProblemsHolder holder = myAnnotationsManager.getCachedProblemHolder(element);
					sum += (SeverityRegistrar.getSeverityRegistrar(getProject()).compare(minSeverity, HighlightSeverity.WARNING) >= 0 ? holder
							.getProblems(element, true, true) : holder.getProblems(element, true, minSeverity)).size();
				}
				status.errorCount[i] = sum;
			}
		}

		private boolean isInspectionCompleted()
		{
			return ContainerUtil.and(myDomElements, element -> myAnnotationsManager.getHighlightStatus(element) == DomHighlightStatus.INSPECTIONS_FINISHED);
		}
	}
}
