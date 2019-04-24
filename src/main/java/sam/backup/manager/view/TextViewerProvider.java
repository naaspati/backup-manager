package sam.backup.manager.view;

import org.codejargon.feather.Provides;

import sam.di.WeakProvider;

public class TextViewerProvider extends WeakProvider<TextViewer> {
    public TextViewerProvider() {
        super(TextViewer::new);
    }
    
    @Override
    @Provides
    public TextViewer get() {
        return w.get();
    }

}
