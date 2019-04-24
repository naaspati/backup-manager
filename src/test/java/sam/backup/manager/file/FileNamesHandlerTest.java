package sam.backup.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Test;

import com.thedeanda.lorem.LoremIpsum;

import sam.functions.IOExceptionConsumer;
import sam.nopkg.Resources;

class FileNamesHandlerTest {

	@Test
	void test() throws IOException {
		Path p = Files.createTempFile(null, null);
		System.out.println(p);
		
		try(Resources r = Resources.get();) {
			LoremIpsum lorem = LoremIpsum.getInstance();
			
			List<String> list1 = new ArrayList<>(1000); 
			write(p, r, lorem, list1, 100);
			
			try(InputStream is = Files.newInputStream(p, StandardOpenOption.READ);
					GZIPInputStream gis = new GZIPInputStream(is);
					InputStreamReader isr = new InputStreamReader(gis, "utf-8");
					BufferedReader reader = new BufferedReader(isr)) {
				
				int n = 0;
				String line;
				while((line = reader.readLine()) != null) 
					assertEquals(list1.get(n++), line);
				
				assertEquals(n, list1.size());
			}
			
			read(p, r, list1);
			
			List<String> list2 = new ArrayList<>();
			Random random = new Random();
			for (int i = 0; i < 100; i++) {
				list2.clear();
				write(p, r, lorem, list2, random.nextInt(100));
				list1.addAll(list2);
				list2.clear();
				
				read(p, r, list1);
			}
		} finally {
			try {
				Files.deleteIfExists(p);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void write(Path p, Resources r, LoremIpsum lorem, List<String> list, int size) throws IOException {
		for (int i = 0; i < size; i++) 
			list.add(lorem.getWords(0, 10));
		
		clear(r);
		
		new FileNamesHandler(p).write(r, list.iterator());
	}

	private void clear(Resources r) {
		StringBuilder sb = r.sb();
		sb.setLength(0);
		r.buffer().clear();
		r.chars().clear();
	}

	private void read(Path p, Resources r, List<String> list) throws IOException {
		clear(r);
		StringBuilder sb = r.sb();
		
		int cap = sb.capacity(); 
		Supplier<int[]> count[] = new Supplier[1];
		
		new FileNamesHandler(p).read(r, new IOExceptionConsumer<String>() {
			int n = 0;
			int chars = 0;
			{
				count[0] = () -> new int[]{this.n, this.chars};
			}
			
			@Override
			public void accept(String t) {
				String e = t == null ? "" : t;
				assertEquals(list.get(n++), e);
				chars += e.length();
			}
		});
		
		int[] n = count[0].get();

		assertEquals(n[0], list.size());
		System.out.println("cap: "+cap+" "+sb.capacity()+", list.size: "+list.size()+", chars: "+n[1]);
	}

}
