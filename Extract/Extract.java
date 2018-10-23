package Extract;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.Adler32;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import Tools.Props;

/*
 * Copyright 2009 Volker Oth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Extraction of resources.
 *
 * @author Volker Oth
 */
public class Extract extends Thread {

	/** file name of extraction configuration */
	final private static String iniName = "extract.ini";
	/** file name of patching configuration */
	final private static String patchIniName = "patch.ini";
	/** file name of resource CRCs (WINLEMM) */
	final private static String crcIniName = "crc.ini";
	/** allows to use this module for creation of the CRC.ini */
	final private static boolean doCreateCRC = false;

	/** index for files to be checked - static since multiple runs are possible */
	private static int checkNo = 0;
	/** index for CRCs - static since multiple runs are possible */
	private static int crcNo = 0;
	/** index for files to be extracted - static since multiple runs are possible */
	private static int extractNo = 0;
	/** index for files to be patched - static since multiple runs are possible */
	private static int patchNo = 0;
	/** array of extensions to be ignored - read from ini */
	private static String ignoreExt[] = {null};
	/** output dialog */
	private static OutputDialog outputDiag;
	/** monitor the files created without erasing the target dir */
	private static HashMap<String,Object> createdFiles;
	/** source path (WINLEMM) for extraction */
	private static String sourcePath;
	/** destination path (Lemmini resource) for extraction */
	private static String destinationPath;
	/** reference path for creation of DIF files */
	private static String referencePath;
	/** path of the DIF files */
	private static String patchPath;
	/** path of the CRC ini (without the file name) */
	private static String crcPath;
	/** exception caught in the thread */
	private static ExtractException threadException = null;
	/** static self reference to access thread from outside */
	private static Thread thisThread;
	/** reference to class loader */
	private static ClassLoader loader;

	/**
	 * Display an exception message box.
	 * @param ex Exception
	 */
	private static void showException(final Throwable ex) {
		String m;
		m = "<html>";
		m += ex.getClass().getName()+"<p>";
		if (ex.getMessage() != null)
			m += ex.getMessage() +"<p>";
		StackTraceElement ste[] = ex.getStackTrace();
		for (int i=0; i<ste.length; i++) {
			m += ste[i].toString()+"<p>";
		}
		m += "</html>";
		ex.printStackTrace();
		JOptionPane.showMessageDialog( null, m, "Error", JOptionPane.ERROR_MESSAGE );
		ex.printStackTrace();
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 *
	 * Extraction running in a Thread.
	 */
	@Override
	public void run() {
		createdFiles = new HashMap<String,Object>(); // to monitor the files created without erasing the target dir

		try {
			// read ini file
			Props props = new Props();
			URL fn = findFile(iniName);
			if (fn==null || !props.load(fn))
				throw new ExtractException("File " + iniName + " not found or error while reading");

			ignoreExt = props.get("ignore_ext", ignoreExt);

			// prolog_ check CRC
			out("\nValidating WINLEMM");
			URL fncrc = findFile(crcIniName);
			Props cprops = new Props();
			if (fncrc==null || !cprops.load(fncrc))
				throw new ExtractException("File " + crcIniName + " not found or error while reading");
			for (int i=0; true; i++) {
				String crcbuf[] = {null,null,null};
				// 0: name, 1:size, 2: crc
				crcbuf = cprops.get("crc_"+Integer.toString(i),crcbuf);
				if (crcbuf[0] == null)
					break;
				out(crcbuf[0]);
				long len = new File(sourcePath+crcbuf[0]).length();
				if (len != Long.parseLong(crcbuf[1]))
					throw new ExtractException("CRC error for file "+sourcePath+crcbuf[0]+".\n");
				byte src[] = readFile(sourcePath+crcbuf[0]);
				Adler32 crc32 = new Adler32();
				crc32.update(src);
				if (Long.toHexString(crc32.getValue()).compareToIgnoreCase(crcbuf[2].substring(2))!=0)
					throw new ExtractException("CRC error for file "+sourcePath+crcbuf[0]+".\n");
				checkCancel();
			}

			// step one: extract the levels
			out("\nExtracting levels");
			for (int i=0; true; i++) {
				String lvls[] = {null,null};
				// 0: srcPath, 1: destPath
				lvls = props.get("level_"+Integer.toString(i),lvls);
				if (lvls[0] == null)
					break;
				extractLevels(sourcePath+lvls[0], destinationPath+lvls[1]);
				checkCancel();
			}

			// step two: extract the styles
			out("\nExtracting styles");
			ExtractSPR sprite = new ExtractSPR();
			for (int i=0; true; i++) {
				String styles[] = {null,null,null,null};
				// 0:SPR, 1:PAL, 2:path, 3:fname
				styles = props.get("style_"+Integer.toString(i),styles);
				if (styles[0] == null)
					break;
				out(styles[3]);
				File dest = new File(destinationPath+styles[2]);
				dest.mkdirs();
				// load palette and sprite
				sprite.loadPalette(sourcePath+styles[1]);
				sprite.loadSPR(sourcePath+styles[0]);
				String files[] = sprite.saveAll(destinationPath+addSeparator(styles[2])+styles[3], false);
				for (int j=0; j<files.length; j++)
					createdFiles.put(files[j].toLowerCase(),null);
				checkCancel();
			}

			// step three: extract the objects
			out("\nExtracting objects");
			for (int i=0; true; i++) {
				String object[] = {null,null,null,null};
				// 0:SPR, 1:PAL, 2:resource, 3:path
				object = props.get("objects_"+Integer.toString(i),object);
				if (object[0] == null)
					break;
				out(object[0]);
				File dest = new File(destinationPath+object[3]);
				dest.mkdirs();
				// load palette and sprite
				sprite.loadPalette(sourcePath+object[1]);
				sprite.loadSPR(sourcePath+object[0]);
				for (int j=0; true; j++) {
					String member[] = {null,null,null};
					// 0:idx, 1:frames, 2:name
					member = props.get(object[2]+"_"+Integer.toString(j),member);
					if (member[0] == null)
						break;
					// save object
					createdFiles.put((destinationPath+addSeparator(object[3])+member[2]).toLowerCase(),null);
					sprite.saveAnim(destinationPath+addSeparator(object[3])+member[2],
							Integer.parseInt(member[0]),Integer.parseInt(member[1]) );
					checkCancel();
				}
			}

			//if (false) { // debug only

			// step four: create directories
			out("\nCreate directories");
			for (int i=0; true; i++) {
				// 0: path
				String path = props.get("mkdir_"+Integer.toString(i),"");
				if (path.length() == 0)
					break;
				out(path);
				File dest = new File(destinationPath+path);
				dest.mkdirs();
				checkCancel();
			}

			// step five: copy stuff
			out("\nCopy files");
			for (int i=0; true; i++) {
				String copy[] = {null,null};
				// 0: srcName, 1: destName
				copy = props.get("copy_"+Integer.toString(i),copy);
				if (copy[0] == null)
					break;
				try {
					copyFile(sourcePath+copy[0], destinationPath+copy[1]);
					createdFiles.put((destinationPath+copy[1]).toLowerCase(),null);
				} catch (Exception ex) {
					throw new ExtractException("Copying "+sourcePath+copy[0]+" to "+destinationPath+copy[1]+ " failed");
				}
				checkCancel();
			}

			// step five: clone files inside destination dir
			out("\nClone files");
			for (int i=0; true; i++) {
				String clone[] = {null,null};
				// 0: srcName, 1: destName
				clone = props.get("clone_"+Integer.toString(i),clone);
				if (clone[0] == null)
					break;
				try {
					copyFile(destinationPath+clone[0], destinationPath+clone[1]);
					createdFiles.put((destinationPath+clone[1]).toLowerCase(),null);
				} catch (Exception ex) {
					throw new ExtractException("Cloning "+destinationPath+clone[0]+" to "+destinationPath+clone[1]+ " failed");
				}
				checkCancel();
			}

			if (referencePath != null) {
				// this is not needed by Lemmini, but to create the DIF files (and CRCs)
				if (doCreateCRC) {
					// create crc.ini
					out("\nCreate CRC ini");
					FileWriter fCRCList;
					try {
						fCRCList = new FileWriter(crcPath+crcIniName);
					} catch (IOException ex) {
						throw new ExtractException(crcPath+crcIniName+" coudn't be openend");
					}
					for (int i=0; true; i++) {
						String ppath;
						ppath = props.get("pcrc_"+Integer.toString(i),"");
						if (ppath.length() == 0)
							break;
						createCRCs(sourcePath, ppath, fCRCList);
					}
					try {
						fCRCList.close();
					} catch (IOException ex) {
						throw new ExtractException(crcPath+crcIniName+" coudn't be closed");
					}
					checkCancel();
				}

				// step seven: create patches and patch.ini
				(new File(patchPath)).mkdirs();
				out("\nCreate patch ini");
				FileWriter fPatchList;
				try {
					fPatchList = new FileWriter(patchPath+patchIniName);
				} catch (IOException ex) {
					throw new ExtractException(patchPath+patchIniName+" coudn't be openend");
				}
				for (int i=0; true; i++) {
					String ppath;
					ppath = props.get("ppatch_"+Integer.toString(i),"");
					if (ppath.length() == 0)
						break;
					createPatches(referencePath, destinationPath, ppath, patchPath, fPatchList);
				}
				try {
					fPatchList.close();
				} catch (IOException ex) {
					throw new ExtractException(patchPath+patchIniName+" coudn't be closed");
				}
				checkCancel();
			}

			// step eight: use patch.ini to extract/patch all files
			// read patch.ini file
			Props pprops = new Props();
			URL fnp = findFile(patchPath+patchIniName/*, this*/); // if it's in the JAR or local directory
			if (!pprops.load(fnp))
				throw new ExtractException("File " + patchIniName + " not found or error while reading");
			// copy
			out("\nExtract files");
			for (int i=0; true; i++) {
				String copy[] = {null,null};
				// 0: name 1: crc
				copy = pprops.get("extract_"+Integer.toString(i),copy);
				if (copy[0] == null)
					break;
				out(copy[0]);
				String fnDecorated = copy[0].replace('/', '@');
				URL fnc = findFile(patchPath+fnDecorated /*, pprops*/);
				try {
					copyFile(fnc, destinationPath+copy[0]);
				} catch (Exception ex) {
					throw new ExtractException("Copying "+patchPath+getFileName(copy[0])+" to "+destinationPath+copy[0]+ " failed");
				}
				checkCancel();
			}
			// patch
			out("\nPatch files");
			for (int i=0; true; i++) {
				String ppath[] = {null,null};
				// 0: name 1: crc
				ppath = pprops.get("patch_"+Integer.toString(i),ppath);
				if (ppath[0] == null)
					break;
				out(ppath[0]);
				String fnDif = ppath[0].replace('/', '@'); //getFileName(ppath[0]);
				int pos = fnDif.toLowerCase().lastIndexOf('.');
				if (pos == -1)
					pos = fnDif.length();
				fnDif = fnDif.substring(0,pos)+".dif";
				URL urlDif = findFile(patchPath+fnDif);
				if (urlDif == null)
					throw new ExtractException("Patching of file "+destinationPath+ppath[0]+" failed.\n");
				byte dif[] = readFile(urlDif);
				byte src[] = readFile(destinationPath+ppath[0]);
				try {
					byte trg[] = Diff.patchbuffers(src, dif);
					// write new file
					writeFile(destinationPath+ppath[0],trg);
				} catch (DiffException ex) {
					throw new ExtractException("Patching of file "+destinationPath+ppath[0]+" failed.\n"+
							ex.getMessage());
				}
				checkCancel();
			}
			//} // debug only

			// finished
			out("\nSuccessfully finished!");
		} catch (ExtractException ex) {
			threadException = ex;
			out(ex.getMessage());
		} catch (Exception ex) {
			showException(ex);
			System.exit(1);
		}  catch (Error ex) {
			showException(ex);
			System.exit(1);
		}
		outputDiag.enableOk();
	}

	/**
	 * Get source path (WINLEMM) for extraction.
	 * @return source path (WINLEMM) for extraction
	 */
	public static String getSourcePath() {
		return sourcePath;
	}

	/**
	 * Get destination path (Lemmini resource) for extraction.
	 * @return destination path (Lemmini resource) for extraction
	 */
	public static String getResourcePath() {
		return destinationPath;
	}

	/**
	 * Extract all resources and create patch.ini if referencePath is not null
	 * @param frame parent frame
	 * @param srcPath WINLEMM directory
	 * @param dstPath target (installation) directory. May also be a relative path inside JAR
	 * @param refPath the reference path with the original (wanted) files
	 * @param pPath the path to store the patch files to
	 * @throws ExtractException
	 */
	public static void extract(final JFrame frame, final String srcPath, final String dstPath, final String refPath, final String pPath) throws ExtractException {

		sourcePath = exchangeSeparators(addSeparator(srcPath));
		destinationPath = exchangeSeparators(addSeparator(dstPath));
		if (refPath != null)
			referencePath = exchangeSeparators(addSeparator(refPath));
		patchPath = exchangeSeparators(addSeparator(pPath));
		crcPath = destinationPath; // ok, this is the wrong path, but this is executed once in a lifetime

		loader = Extract.class.getClassLoader();

		FolderDialog fDiag;
		do {
			fDiag = new FolderDialog(frame, true);
			fDiag.setParameters(sourcePath, destinationPath);
			fDiag.setVisible(true);
			if (!fDiag.getSuccess())
				throw new ExtractException("Extraction cancelled by user");
			sourcePath = exchangeSeparators(addSeparator(fDiag.getSource()));
			destinationPath = exchangeSeparators(addSeparator(fDiag.getTarget()));
			// check if source path exists
			File fSrc = new File(sourcePath);
			if (fSrc.exists())
				break;
			JOptionPane.showMessageDialog(frame,"Source path "+sourcePath+" doesn't exist!","Error",JOptionPane.ERROR_MESSAGE);
		} while (true);

		// open output dialog
		outputDiag = new OutputDialog(frame, true);

		// start thread
		threadException = null;
		thisThread = new Thread(new Extract());
		thisThread.start();

		outputDiag.setVisible(true);
		while (thisThread.isAlive()) {
			try  {
				Thread.sleep(200);
			} catch (InterruptedException ex) {}
		}
		if (threadException != null)
			throw threadException;
	}

	/**
	 * Extract the level INI files from LVL files
	 * @param r name of root folder (source of LVL files)
	 * @param dest destination folder for extraction (resource folder)
	 * @throws ExtractException
	 */
	private static void extractLevels(final String r, final String destin) throws ExtractException {
		// first extract the levels
		File fRoot = new File(r);
		FilenameFilter ff = new LvlFilter();

		String root = addSeparator(r);
		String destination = addSeparator(destin);
		File dest = new File(destination);
		dest.mkdirs();

		File[] levels = fRoot.listFiles(ff);
		if (levels == null)
			throw new ExtractException("Path "+root+" doesn't exist or IO error occured.");
		for (int i = 0; i< levels.length; i++) {
			int pos;
			String fIn = root + levels[i].getName();
			String fOut = levels[i].getName();
			pos = fOut.toLowerCase().indexOf(".lvl"); // MUST be there because of file filter
			fOut = destination + (fOut.substring(0,pos)+".ini").toLowerCase();
			createdFiles.put(fOut.toLowerCase(),null);
			try {
				out(levels[i].getName());
				ExtractLevel.convertLevel(fIn, fOut);
			} catch (Exception ex) {
				String msg = ex.getMessage();
				if (msg!=null && msg.length()>0)
					out(ex.getMessage());
				else
					out(ex.toString());
				throw new ExtractException(msg);
			}
		}
	}

	/**
	 * Create the DIF files from reference files and the extracted files (development).
	 * @param sPath The path with the original (wanted) files
	 * @param dPath  The patch with the differing (to be patched) files
	 * @param subDir SubDir to create patches for
	 * @param pPath  The patch to write the patches to
	 * @param fPatchList FileWriter to create patch.ini
	 * @throws ExtractException
	 */
	private static void createPatches(final String sPath, final String dPath, final String sDir, final String pPath, final FileWriter fPatchList) throws ExtractException {
		// add separators and create missing directories
		sourcePath = addSeparator(sPath+sDir);
		File fSource = new File(sourcePath);

		String destPath = addSeparator(dPath+sDir);
		File fDest = new File(destPath);
		fDest.mkdirs();

		String out;
		patchPath = addSeparator(pPath);
		File fPatch = new File(patchPath);
		fPatch.mkdirs();

		File[] files = fSource.listFiles();
		if (files == null)
			throw new ExtractException("Path "+sourcePath+" doesn't exist or IO error occured.");
		Diff.setParameters(512,4);
		String subDir = addSeparator(sDir);
		String subDirDecorated = subDir.replace('/', '@');

		outerLoop:
			for (int i = 0; i< files.length; i++) {
				int pos;
				// ignore directories
				if (files[i].isDirectory())
					continue;
				// check extension
				pos = files[i].getName().lastIndexOf('.');
				if (pos > -1) {
					String ext = files[i].getName().substring(pos+1);
					for (int n=0; n<ignoreExt.length; n++)
						if (ignoreExt[n].equalsIgnoreCase(ext))
							continue outerLoop;
				}

				String fnIn = sourcePath + files[i].getName();
				String fnOut = destPath + files[i].getName();
				String fnPatch = files[i].getName();

				pos = fnPatch.toLowerCase().lastIndexOf('.');
				if (pos == -1)
					pos = fnPatch.length();
				fnPatch = patchPath + subDirDecorated + (fnPatch.substring(0,pos)+".dif").toLowerCase();
				try {
					out(fnIn);
					// read src file
					byte src[] = readFile(fnIn);
					byte trg[] = null;
					// read target file
					boolean fileExists;
					if (createdFiles.containsKey(fnOut.toLowerCase()))
						fileExists = true;
					else
						fileExists = false;
					if (fileExists) {
						try {
							trg = readFile(fnOut);
						} catch (ExtractException ex) {
							fileExists = false;
						}
					}
					if (!fileExists) {
						// mark missing files: needs to be extracted from JAR
						Adler32 crc = new Adler32();
						crc.update(src);
						out = subDir+files[i].getName()+", 0x"+Integer.toHexString((int)crc.getValue());
						fPatchList.write("extract_"+(Integer.toString(extractNo++))+" = "+out+"\n");
						// copy missing files to patch dir
						copyFile(fnIn,  patchPath + subDirDecorated + files[i].getName());
						continue;
					}
					// create diff
					byte patch[] = Diff.diffBuffers(trg,src);
					int crc = Diff.targetCRC; // crc of target buffer
					out = subDir+files[i].getName()+", 0x"+Integer.toHexString(crc);
					if (patch == null) {
						//out("src and trg are identical");
						fPatchList.write("check_"+(Integer.toString(checkNo++))+" = "+out+"\n");
					}
					else {
						// apply patch to test it's ok
						Diff.patchbuffers(trg,patch);
						// write patch file
						writeFile( fnPatch, patch);
						fPatchList.write("patch_"+(Integer.toString(patchNo++))+" = "+out+"\n");
					}
				} catch (Exception ex)  {
					String msg = ex.getMessage();
					if (msg == null)
						msg = ex.toString();
					throw new ExtractException(ex.getMessage());
				}
			}
	}

	/**
	 * Create CRCs for resources (development).
	 * @param rPath The root path with the files to create CRCs for
	 * @param sDir SubDir to create patches for
	 * @param fCRCList FileWriter to create crc.ini
	 * @throws ExtractException
	 */
	private static void createCRCs(final String rPath, final String sDir, final FileWriter fCRCList) throws ExtractException {
		// add separators and create missing directories
		String rootPath = addSeparator(rPath+sDir);
		File fSource = new File(rootPath);
		String out;
		File[] files = fSource.listFiles();
		if (files == null)
			throw new ExtractException("Path "+rootPath+" doesn't exist or IO error occured.");
		String subDir = addSeparator(sDir);

		outerLoop:
			for (int i = 0; i< files.length; i++) {
				int pos;
				// ignore directories
				if (files[i].isDirectory())
					continue;
				// check extension
				pos = files[i].getName().lastIndexOf('.');
				if (pos > -1) {
					String ext = files[i].getName().substring(pos+1);
					for (int n=0; n<ignoreExt.length; n++)
						if (ignoreExt[n].equalsIgnoreCase(ext))
							continue outerLoop;
				}
				String fnIn = rootPath + files[i].getName();
				try {
					out(fnIn);
					// read src file
					byte src[] = readFile(fnIn);
					Adler32 crc32 = new Adler32();
					crc32.update(src);
					out = subDir+files[i].getName()+", "+src.length+", 0x"+Long.toHexString(crc32.getValue());
					fCRCList.write("crc_"+(Integer.toString(crcNo++))+" = "+out+"\n");
				} catch (Exception ex)  {
					String msg = ex.getMessage();
					if (msg == null)
						msg = ex.toString();
					throw new ExtractException(ex.getMessage());
				}
			}
	}


	/**
	 * Add separator "/" to path name (if there isn't one yet)
	 * @param fName path name with or without separator
	 * @return path name with separator
	 */
	private static String addSeparator(final String fName) {
		int pos = fName.lastIndexOf(File.separator);
		if (pos != fName.length()-1)
			pos = fName.lastIndexOf("/");
		if (pos != fName.length()-1)
			return fName + "/";
		else return fName;
	}

	/**
	 * Exchange all Windows style file separators ("\") with Unix style seaparators ("/")
	 * @param fName file name
	 * @return file name with only Unix style separators
	 */
	private static String exchangeSeparators(final String fName) {
		int pos;
		StringBuffer sb = new StringBuffer(fName);
		while ( (pos = sb.indexOf("\\")) != -1 )
			sb.setCharAt(pos,'/');
		return sb.toString();
	}

	/**
	 * Get only the name of the file from an absolute path.
	 * @param path absolute path of a file
	 * @return file name without the path
	 */
	private static String getFileName(final String path) {
		int p1 = path.lastIndexOf("/");
		int p2 = path.lastIndexOf("\\");
		if (p2 > p1)
			p1 = p2;
		if (p1 < 0)
			p1 = 0;
		else
			p1++;
		return path.substring(p1);
	}

	/**
	 * Copy a file.
	 * @param source URL of source file
	 * @param destination full destination file name including path
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static void copyFile(final URL source, final String destination) throws FileNotFoundException, IOException {
		InputStream fSrc = source.openStream();
		FileOutputStream fDest = new FileOutputStream(destination);
		byte buffer[] = new byte[4096];
		int len;

		while ( (len=fSrc.read(buffer)) != -1)
			fDest.write(buffer,0,len);
		fSrc.close();
		fDest.close();
	}

	/**
	 * Copy a file.
	 * @param source full source file name including path
	 * @param destination full destination file name including path
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static void copyFile(final String source, final String destination) throws FileNotFoundException, IOException {
		FileInputStream fSrc = new FileInputStream(source);
		FileOutputStream fDest = new FileOutputStream(destination);
		byte buffer[] = new byte[4096];
		int len;

		while ( (len=fSrc.read(buffer)) != -1)
			fDest.write(buffer,0,len);
		fSrc.close();
		fDest.close();
	}

	/**
	 * Read file into an array of byte.
	 * @param fname file name
	 * @return array of byte
	 * @throws ExtractException
	 */
	private static byte[] readFile(final String fname) throws ExtractException {
		byte buf[] = null;
		try {
			int len = (int)(new File(fname).length());
			FileInputStream f = new FileInputStream(fname);
			buf = new byte[ len ];
			f.read( buf );
			f.close();
			return buf;
		} catch (FileNotFoundException ex) {
			throw new ExtractException("File "+fname+" not found");
		} catch (IOException ex) {
			throw new ExtractException("IO exception while reading file "+fname);
		}
	}

	/**
	 * Read file into an array of byte.
	 * @param fname file name as URL
	 * @return array of byte
	 * @throws ExtractException
	 */
	private static byte[] readFile(final URL fname) throws ExtractException {
		byte buf[] = null;
		try {
			InputStream f = fname.openStream();
			byte buffer[] = new byte[4096];
			// URLs/InputStreams suck: we can't read a length
			int len;
			ArrayList<Byte> lbuf = new ArrayList<Byte>();

			while ( (len=f.read(buffer)) != -1) {
				for (int i=0; i<len; i++)
					lbuf.add(new Byte(buffer[i]));
			}
			f.close();

			// reconstruct byte array from ArrayList
			buf = new byte[lbuf.size()];
			for (int i=0; i<lbuf.size(); i++)
				buf[i] = lbuf.get(i).byteValue();

			return buf;
		} catch (FileNotFoundException ex) {
			throw new ExtractException("File "+fname+" not found");
		} catch (IOException ex) {
			throw new ExtractException("IO exception while reading file "+fname);
		}
	}

	/**
	 * Write array of byte to file.
	 * @param fname file name
	 * @param buf array of byte
	 * @throws ExtractException
	 */
	private static void writeFile(final String fname, final byte buf[]) throws ExtractException {
		try {
			FileOutputStream f = new FileOutputStream(fname);
			f.write( buf );
			f.close();
		} catch (IOException ex) {
			throw new ExtractException("IO exception while writing file "+fname);
		}
	}

	/**
	 * Find a file.
	 * @param fname File name (without absolute path)
	 * @return URL to file
	 */
	public static URL findFile(final String fname) {
		URL retval = loader.getResource(fname);
		try {
			if (retval==null)
				retval = new File(fname).toURI().toURL();
			return retval;
		} catch (MalformedURLException ex) {}
		return null;
	}

	/**
	 * Print string to output dialog.
	 * @param s string to print
	 */
	private static void out(final String s) {
		// System.out.println(s);
		if (outputDiag != null)
			outputDiag.print(s+"\n");
	}

	/**
	 * Return cancel state of output dialog
	 * @throws ExtractException
	 */
	private static void checkCancel() throws ExtractException {
		if (outputDiag.isCancelled())
			throw new ExtractException("Extraction cancelled by user");
	}
}

/**
 * File name filter for level files.
 * @author Volker Oth
 */
class LvlFilter implements FilenameFilter {

	/* (non-Javadoc)
	 * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
	 */
	@Override
	public boolean accept(final File dir, final String name) {
		if (name.toLowerCase().indexOf(".lvl") != -1)
			return true;
		else
			return false;
	}
}

