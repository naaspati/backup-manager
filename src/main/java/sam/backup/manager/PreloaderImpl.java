package sam.backup.manager;

import org.apache.logging.log4j.LogManager;

import javafx.application.Preloader;
import javafx.application.Preloader.StateChangeNotification.Type;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import sam.myutils.MyUtilsException;
import sam.thread.MyUtilsThread;

public class PreloaderImpl extends Preloader {
	public static class Progress implements PreloaderNotification {
		final double value;
		final String details;

		public Progress(double value, String details) {
			this.value = value;
			this.details = details;
		}
	}

	private ProgressBar bar = new ProgressBar();
	private Stage stage;
	private TextArea status = new TextArea();

	@Override
	public void start(Stage stage) throws Exception {
		stage.setScene(new Scene(new HBox(bar)));
		stage.show();
		this.stage = stage;
	}

	@Override
	public void handleProgressNotification(ProgressNotification info) {
		if(info == null)
			return;
		bar.setProgress(info.getProgress());
	}

	@Override
	public void handleStateChangeNotification(StateChangeNotification info) {
		if(info.getType() == Type.BEFORE_START)
			stage.hide();
	}

	@Override
	public void handleApplicationNotification(PreloaderNotification info) {
		if(info == null)
			return;

		if(info instanceof ProgressNotification) 
			bar.setProgress(((ProgressNotification)info).getProgress());
		else if(info instanceof Progress)  {
			Progress p = (Progress)info;
			bar.setProgress(p.value);
			status.appendText(p.details+"\n");
		} else {
			System.out.println(info);
		}
	}

	@Override
	public boolean handleErrorNotification(ErrorNotification info) {
		stage.hide();
		StringBuilder sb = new StringBuilder();
		sb.append(info.getLocation()).append('\n')
		.append(info.getDetails()).append('\n');
		
		LogManager.getRootLogger().error("failed to app: \nlocation: "+info.getLocation()+"\ndetails: "+info.getDetails(), info.getCause());
		
		MyUtilsException.append(sb, info.getCause(), true);
		
		Stage stage = new Stage(StageStyle.UTILITY);
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.initOwner(this.stage);
		
		stage.setScene(new Scene(new TextArea(sb.toString())));
		stage.showAndWait();
		
		return true;
	}

	@Override
	public void stop() throws Exception {
		MyUtilsThread.printstackLocation();
	}

}
