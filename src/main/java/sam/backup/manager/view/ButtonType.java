package sam.backup.manager.view;

public enum ButtonType {
	OK,
	CANCEL, 
	WALK, 
	UPLOAD, 
	OPEN,
	SAVE, 
	FILES, 
	LIST_VIEW,
	TREE_VIEW, 
	SET_MODIFIED,
	LOADING, 
	DELETE, ;

	private ButtonType() {
		this(null);
	}
	public final String cssClass, text;
	private ButtonType(String text) {
		this.cssClass = toString().toLowerCase().concat("-btn");
		if(text == null) {
			char[] cs = this.toString().toCharArray();
			for (int i = 1; i < cs.length; i++) {
				if(cs[i] == '_')
					cs[i] = ' ';
				else if(cs[i - 1] != ' ')
					cs[i] = Character.toLowerCase(cs[i]);
			}
			this.text = new String(cs);
		}
		else 
			this.text = text;
	}
}