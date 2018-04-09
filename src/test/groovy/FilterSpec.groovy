import java.nio.file.Path
import java.nio.file.Paths

import com.google.gson.Gson

import sam.backup.manager.config.filter.Filter
import spock.lang.*

class FilterSpec extends Specification {
	def "json is parsing well"() {
		given: 'a json text'
		String json = new File('src/test/resources/filter-json-1.json').text

		and: 'a parser'
		Gson parser = new Gson();

		when: 'json is parsed'
		Filter filter =  parser.fromJson(json, Filter)

		and:
		Map map = filter.getArrays()

		then: 'json filter values must be equal to expected values'
		map[key] == value;

		where: ' expected parsed key-values are'
		key    || value
		'name' || ['eclipse_workplace', '.gradle', '.sass-cache', 'bin', 'build', 'current_session', 'node_modules', 'noter.zip', 'RemoteSystemsTempFiles', 'thumbs', 'typings', 'yarn.lock' ]
		'glob' || ['*.class']
		'path' || [/C:\Users\Sameer\Documents\MEGA\Rubbish/]
	}

	def "filter is should pass"() {
		given: 'a json text'
		String json = new File('src/test/resources/filter-json-1.json').text

		and: 'a parser'
		Gson parser = new Gson();

		when: 'json is parsed'
		Filter filter =  parser.fromJson(json, Filter)

		then: 'json filter return true with given value'
		filter.test(path)

		where: ' expected parsed key-values are'
		path << paths()
	}
	
	def "filter is should fail"() {
		given: 'a json text'
		String json = new File('src/test/resources/filter-json-1.json').text

		and: 'a parser'
		Gson parser = new Gson();

		when: 'json is parsed'
		Filter filter =  parser.fromJson(json, Filter)

		then: 'json filter return false with given value'
		!filter.test(path)

		where: ' expected parsed key-values are'
		path << paths().collect{Paths.get(it.toString()+"1")}
	}

	def paths() {
		def d =	['eclipse_workplace', '.gradle', '.sass-cache', 'bin', 'build', 'current_session', 'node_modules', 'noter.zip', 'RemoteSystemsTempFiles', 'thumbs', 'typings', 'yarn.lock', 'anime.class' ].collect{/C:\Users\Sameer\Documents\MEGA\Rubbish\$it/}.collect{Paths.get(it)}
		d << Paths.get(/C:\Users\Sameer\Documents\MEGA\Rubbish/)
		d
	}
}
