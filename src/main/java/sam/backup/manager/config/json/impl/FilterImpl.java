package sam.backup.manager.config.json.impl;

import java.nio.file.Path;

import sam.backup.manager.config.api.Filter;

class FilterImpl extends Filter {
	private ConfigImpl config;

	public void setConfig(ConfigImpl config) {
		this.config  = config;
		if(invert != null)
			((FilterImpl)invert).setConfig(config);
	}

	@Override
	protected Path resolve(String path) {
		return config.resolve(path);
	}

}
