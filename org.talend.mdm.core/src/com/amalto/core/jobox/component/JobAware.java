package com.amalto.core.jobox.component;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amalto.core.jobox.JobInfo;
import com.amalto.core.jobox.util.JoboxConfig;

public class JobAware {
	public static final Pattern jobVersionNamePattern = Pattern.compile("(.*?)_(\\d*.\\d*)$");
	private static final String JOBOX_RESERVED_FOLDER_NAME = "tmp";
	
	private String workDir;
    private String contextStr;
    
	public JobAware(JoboxConfig joboxConfig) {
		this.workDir=joboxConfig.getWorkPath();
	}

	/**
	 * @return
	 */
	public List<JobInfo> findJobsInBox() {
		File[] entities = new File(workDir).listFiles(new FileFilter() {

			public boolean accept(File pathname) {
				if (pathname.isFile()
						|| pathname.getName().equalsIgnoreCase(
								JOBOX_RESERVED_FOLDER_NAME)) {
					return false;
				}
				return true;
			}

		});

		List<JobInfo> jobList = new ArrayList<JobInfo>();
		for (int i = 0; i < entities.length; i++) {
			File entity = entities[i];
			boolean isTISEntry = recognizeTISJob(entity);
			if (isTISEntry) {

				// parse name and version
				String jobVersion = "";
				String jobName = "";
				Matcher m = jobVersionNamePattern.matcher(entity.getName());
				while (m.find()) {
					jobName = m.group(1);
					jobVersion = m.group(m.groupCount());
				}

				JobInfo jobInfo = new JobInfo(jobName, jobVersion);
				setClassPath4TISJob(entity,jobInfo);
				String propFilePath=analyzeJobParams(entity, jobInfo);
				guessMainClass(entity,propFilePath,jobInfo);
				jobList.add(jobInfo);
			}
		}
		return jobList;
	}

	public JobInfo loadJobInfo(String entityName) {
		JobInfo jobInfo = null;
		File entity=new File(workDir+File.separator+entityName);
		if(entity.exists()) {
			// parse name and version
			String jobVersion = "";
			String jobName = "";
			Matcher m = jobVersionNamePattern.matcher(entityName);
			while (m.find()) {
				jobName = m.group(1);
				jobVersion = m.group(m.groupCount());
			}
			
			jobInfo = new JobInfo(jobName, jobVersion);
			setClassPath4TISJob(entity,jobInfo);
			String propFilePath=analyzeJobParams(entity, jobInfo);
			guessMainClass(entity,propFilePath,jobInfo);
		}
		return jobInfo;
	}
	
	private void guessMainClass(File entity,String propFilePath,JobInfo jobInfo) {
		//FIXME:THIS WAY IS NOT GOOD
		if(propFilePath!=null) {
			String jobName=jobInfo.getName();
			String className="";
			String packagename="";
			String splitTag="/";
			if(File.separator.equals("\\"))splitTag="\\\\";
			String[] parts=propFilePath.split(splitTag);
			boolean startRecord=false;
			for (int i = 0; i < parts.length; i++) {
				if(parts[i].equals("contexts")) {
					startRecord=false;
					break;
				}
				if(parts[i].equals(jobName)&&!startRecord) {
					startRecord=true;
					continue;
				}
				if(startRecord) {
					if(packagename.length()==0)packagename+=parts[i];
					else if(packagename.length()>0)packagename+="."+parts[i];
				}
			}
			if(packagename.length()>0)className=packagename+"."+jobName;
			
			if(className.length()>0)jobInfo.setMainclass(className);
		}
	}

	public void setContextStr(String cxt){
	    contextStr = cxt;
	}
	
	private String analyzeJobParams(File entity, JobInfo jobInfo) {
		String propFilePath=null;
		try {
			List<File> checkList = new ArrayList<File>();
			findFirstFile(jobInfo,entity, (contextStr == null ? "Default" :  contextStr)+".properties", checkList);//$NON-NLS-1$//$NON-NLS-2$
			if (checkList.size() > 0) {
				propFilePath=checkList.get(0).getAbsolutePath();
				Properties paramProperties = new Properties();
				FileInputStream fileReader = new FileInputStream(checkList.get(0));
				paramProperties.load(fileReader);
				if (fileReader != null)
					fileReader.close();

				for (Enumeration e = paramProperties.propertyNames(); e.hasMoreElements();)
				{
					String key = (String) e.nextElement();
					String value = paramProperties.getProperty(key);
					jobInfo.addParam(key, value);
				}

			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return propFilePath;
	}

	private  boolean recognizeTISJob(File entity) {
		boolean isTISEntry = false;
		List<File> checkList = new ArrayList<File>();
		findFirstFile(null, entity, "classpath.jar", checkList);
		if (checkList.size() > 0) {
			try {
				JarFile jarFile = new JarFile(checkList.get(0)
						.getAbsolutePath());
				Manifest jarFileManifest = jarFile.getManifest();
				String vendorInfo = jarFileManifest.getMainAttributes().getValue("Implementation-Vendor");
				if (vendorInfo.trim().toUpperCase().startsWith("TALEND"))
					isTISEntry = true;
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		return isTISEntry;
	}
	
	private void setClassPath4TISJob(File entity,JobInfo jobInfo) {
		
		String newClassPath="";
		String separator = System.getProperty("path.separator");
		
		List<File> checkList = new ArrayList<File>();
		findFirstFile(null,entity, "classpath.jar", checkList);
		if (checkList.size() > 0) {
			try {
				String basePath=checkList.get(0).getParent();
				JarFile jarFile = new JarFile(checkList.get(0).getAbsolutePath());
				Manifest jarFileManifest = jarFile.getManifest();
				String classPaths = jarFileManifest.getMainAttributes().getValue("Class-Path");
				String[] classPathsArray=classPaths.split("\\s+",0);
				List<String> classPathsArrayList=new ArrayList<String>(Arrays.asList(classPathsArray));
				List<String> classPathsExtArray=new ArrayList<String>();
				if(!classPathsArrayList.contains("."))classPathsExtArray.add(".");
				if(classPathsArrayList.size()>0)classPathsExtArray.addAll(classPathsArrayList);
				for (int i = 0; i < classPathsExtArray.size(); i++) {
					String classPath=classPathsExtArray.get(i);
					File libFile=new File(basePath+File.separator+classPath);;
					if(libFile.exists()) {
						if(newClassPath.length()==0)newClassPath+=libFile.getAbsolutePath();
						else if(newClassPath.length()>0)newClassPath+=separator+libFile.getAbsolutePath();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		jobInfo.setClasspath(newClassPath);
	}

	private void findFirstFile(JobInfo jobInfo,File root, String fileName,
			List<File> resultList) {

		if (resultList.size() > 0)
			return;

		if (root.isFile()) {			
			if (root.getName().equals(fileName)) {
				if(jobInfo==null || root.getParentFile().getParentFile().getName().toLowerCase().startsWith(jobInfo.getName().toLowerCase()))
					resultList.add(root);
			}
		} else if (root.isDirectory()) {
			File[] files = root.listFiles();
			for (int i = 0; i < files.length; i++) {
				findFirstFile(jobInfo,files[i], fileName, resultList);
			}
		}

	}
	
	

}
