package tools.pdmgenerate;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Starter {
	private static final Logger log = LoggerFactory.getLogger(Starter.class);

	public static void main(String[] args) throws Exception {
		String filename = (args.length == 0 ? "config.yml" : args[0]);
		log.info("load config file: " + filename);
		Configuration.load(filename);

		FileUtils.deleteQuietly(new File(Configuration.GENERATE_DIR));
		new ToAutoIncrementSQL().handle();
		new ToEntity().handle();
		new ToIbatis2(IbatisType.BASE).handle();
		new ToIbatis2(IbatisType.EXTEND).handle();
		new ToDao().handle();
		new ToJsp().handle();
		new ToMeta().handle();
		new ToQuery().handle();
		new ToDTO().handle();
		new ToService().handle();
		log.info("generate dir: " + Configuration.GENERATE_DIR);
		log.info("generate success.");
	}

}