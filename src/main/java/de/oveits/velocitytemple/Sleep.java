package de.oveits.velocitytemple;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.camel.Body;
import org.apache.camel.Header;
import org.apache.camel.util.FileUtil;

public class Sleep {		
	public void perform(@Header("sleep") Integer milliseconds) throws InterruptedException  {
		Thread.sleep(milliseconds);
	    }
}
