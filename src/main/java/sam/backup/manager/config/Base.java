package sam.backup.manager.config;

public abstract class Base {
	private String[] excludes;
	private String[] targetExcludes;
	private boolean onlyExistsCheck;
	
	private transient Exclude excluder, targetExcluder;
	private transient boolean modified = false;
	
	public boolean isOnlyExistsCheck() {
		return onlyExistsCheck;
	}
	public boolean isModified() {
		return modified;
	}
	protected void setModified() {
		this.modified = true;
	}
	public Exclude getSourceExcluder() {
		if(excludes == null) return null;
		if(excluder == null) excluder = new Exclude(excludes);
		return excluder;
	}
	public Exclude getTargetExcluder() {
		if(targetExcludes == null) return null;
		if(targetExcluder == null) targetExcluder = new Exclude(targetExcludes);
		return targetExcluder;
	}
	
	/*
	 public void addExclude(String exclude) {
		excluder = null;
		excludes = addValue(excludes, exclude);
	}
	 */
	
	/*
	@SuppressWarnings("unchecked")
	protected <E> E[] addValue(E[] currentValues, E newValue) {
		Objects.requireNonNull(newValue);
		setModified();
		
		currentValues = currentValues == null ? (E[]) Array.newInstance(newValue.getClass().getComponentType() == null ? newValue.getClass() : newValue.getClass().getComponentType(), 1) : Arrays.copyOf(currentValues, currentValues.length + 1);
		currentValues[currentValues.length - 1] = newValue;
		
		return currentValues;
	}
	 */
}
