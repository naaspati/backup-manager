	@SuppressWarnings({"unchecked", "rawtypes"})
	private class InjectorImpl implements Injector {
		final Feather feather;
		final Map<Class, Class> mapping ;
		
		public InjectorImpl(Map<Class, Class> mapping) throws IOException, ClassNotFoundException {
			this.mapping = Checker.isEmpty(mapping) ? Collections.emptyMap() : Collections.unmodifiableMap(mapping);
			this.feather = Feather.with(this);
		}

		@Override
		public <E> E instance(Class<E> type) {
			return feather.instance(map(type));
		}
		@Override
		public <E, F extends Annotation> E instance(Class<E> type, Class<F> qualifier) {
			return feather.instance(Key.of(map(type), qualifier));
		}
		private <E> Class<E> map(Class<E> type) {
			return mapping.getOrDefault(type, type);
		}
		@Provides
		private AppConfig config() {
			return appConfig;
		}
		@Provides
		private FileStoreManager fsm() {
			return fileStoreManager;
		}
		@Provides
		private Injector me() {
			return this;
		}
		@Provides
		private ConfigManager configManager() {
			return configManager;
		}
		@Provides
		private FileTreeManager filtreeFactory() {
			return fileTreeFactory;
		}
		@Provides
		private IUtils getUtils() {
			return utils;
		}
		@Provides
		private IUtilsFx getFx() {
			return fx;
		}
		@Provides
		@Backups
		private Collection<Config> backups() {
			return configManager.get(ConfigType.BACKUP);
		}
		@Provides
		@Lists
		private Collection<Config> lists() {
			return configManager.get(ConfigType.LIST);
		}
		@Provides
		private Executor executor() {
			return App.this;
		}
	}