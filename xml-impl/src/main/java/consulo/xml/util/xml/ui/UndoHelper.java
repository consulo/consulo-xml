/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.xml.util.xml.ui;

import consulo.document.Document;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.language.psi.PsiDocumentManager;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.event.CommandAdapter;
import consulo.undoRedo.event.CommandEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * @author peter
 */
public class UndoHelper {
  private final Project myProject;
  private boolean myShowing;
  private final Set<Document> myCurrentDocuments = new HashSet<Document>();
  private boolean myDirty;
  private final DocumentAdapter myDocumentAdapter = new DocumentAdapter() {
    public void documentChanged(DocumentEvent e) {
      if (myShowing) {
        myDirty = true;
      }
    }
  };

  public UndoHelper(final Project project, final Committable committable) {
    myProject = project;
    final PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
    CommandProcessor.getInstance().addCommandListener(new CommandAdapter() {
      public void commandStarted(CommandEvent event) {
        undoTransparentActionStarted();
      }

      public void undoTransparentActionStarted() {
        myDirty = false;
      }

      public void undoTransparentActionFinished() {
        if (myDirty) {
          psiDocumentManager.commitAllDocuments();
          committable.reset();
        }
      }

      public void commandFinished(CommandEvent event) {
        undoTransparentActionFinished();
      }
    }, committable);
  }

  public final void startListeningDocuments() {
    for (final Document document : myCurrentDocuments) {
      document.addDocumentListener(myDocumentAdapter);
    }
  }

  public final void stopListeningDocuments() {
    for (final Document document : myCurrentDocuments) {
      document.removeDocumentListener(myDocumentAdapter);
    }
  }

  public final void setShowing(final boolean showing) {
    commitAllDocuments();
    myShowing = showing;
  }

  public boolean isShowing() {
    return myShowing;
  }

  public final void commitAllDocuments() {
    final PsiDocumentManager manager = getDocumentManager();
    for (final Document document : myCurrentDocuments) {
      manager.commitDocument(document);
    }
  }

  private PsiDocumentManager getDocumentManager() {
    return PsiDocumentManager.getInstance(myProject);
  }

  public final void addWatchedDocument(final Document document) {
    stopListeningDocuments();
    myCurrentDocuments.add(document);
    startListeningDocuments();
  }

  public final void removeWatchedDocument(final Document document) {
    stopListeningDocuments();
    myCurrentDocuments.remove(document);
    startListeningDocuments();
  }

  public final Document[] getDocuments() {
    return myCurrentDocuments.toArray(new Document[myCurrentDocuments.size()]);
  }


}
