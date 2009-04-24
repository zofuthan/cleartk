 /** 
 * Copyright (c) 2007-2008, Regents of the University of Colorado 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 
 * Neither the name of the University of Colorado at Boulder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. 
*/
package org.cleartk.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.uutuc.factory.CollectionReaderFactory;
import org.uutuc.util.JCasIterable;

/**
 * <br>Copyright (c) 2007-2008, Regents of the University of Colorado 
 * <br>All rights reserved.

 *
 * Unit tests for org.cleartk.readers.DirectoryCollectionReader.
 * 
 * @author Steven Bethard
 */
public class FilesCollectionReaderTests {
	
	/**
	 * The directory containing all the files to be loaded.
	 */
	private final String inputDir = "test/data/html";
	
	/**
	 * The paths for the files in the input directory.
	 */
	private final String[] paths = new String[]{
			"/1.html",
			"/2/2.1.html",
			"/2/2.2.html",
			"/3.html",
			"/4/1/4.1.1.html",
	};

	private final String[] pathsSuffix1 = new String[]{
			"/1.html",
			"/2/2.1.html",
			"/4/1/4.1.1.html",
	};

	private final String[] pathsSuffix2 = new String[]{
			"/1.html",
			"/2/2.1.html",
			"/2/2.2.html",
			"/4/1/4.1.1.html",
	};

	/**
	 * Test that the text loaded into the CAS by the CollectionReader matches
	 * the text in the files on disk.
	 * 
	 * @throws IOException
	 * @throws UIMAException
	 */
	@Test
	public void testText() throws Exception {
		
		// create the PlainTextCollectionReader with the HTML input directory
		String languageCode = "en-us";
		CollectionReader reader = CollectionReaderFactory.createCollectionReader(
				FilesCollectionReader.class, null,
				FilesCollectionReader.PARAM_FILE_OR_DIRECTORY, this.inputDir,
				FilesCollectionReader.PARAM_LANGUAGE, languageCode);
		Assert.assertEquals(0, reader.getProgress()[0].getCompleted());

		// check that each document in the CAS matches the document on disk
		for (JCas jCas: new JCasIterable(reader)) {
			Assert.assertEquals(languageCode, jCas.getDocumentLanguage());
			
			String jCasText = jCas.getDocumentText();
			String docText = this.getFileText(jCas);
			Assert.assertEquals(jCasText, docText);
		}
		reader.close();
		Assert.assertEquals(this.paths.length, reader.getProgress()[0].getCompleted());
	}
	
	/**
	 * Test that that the CollectionReader can load the text into different
	 * CAS views when requested.
	 * 
	 * @throws IOException
	 * @throws UIMAException
	 */
	@Test
	public void testViewText() throws Exception {
		
		// check texts when different views are used
		for (String viewName: new String[]{"TestView", "OtherTestView"}) {
			
			// create the PlainTextCollectionReader with the current view name
			CollectionReader reader = CollectionReaderFactory.createCollectionReader(
					FilesCollectionReader.class, null,
					FilesCollectionReader.PARAM_FILE_OR_DIRECTORY, this.inputDir,
					FilesCollectionReader.PARAM_VIEW_NAME, viewName);
			
			// check that each document in the JCas views matches the document on disk
			for (JCas jCas: new JCasIterable(reader)) {
				JCas view = jCas.getView(viewName);
				String jCasText = view.getDocumentText();
				String docText = this.getFileText(view);
				Assert.assertEquals(jCasText, docText);
			}
			reader.close();
		}
	}
	
	/**
	 * Test that all files in the directory (and subdirectories) are
	 * loaded into the CAS.
	 * 
	 * @throws IOException
	 * @throws UIMAException
	 */
	@Test
	public void testFilePaths() throws IOException, UIMAException {
		
		// create the PlainTextCollectionReader with the HTML input directory  
		CollectionReader reader = CollectionReaderFactory.createCollectionReader(
				FilesCollectionReader.class, null,
				FilesCollectionReader.PARAM_FILE_OR_DIRECTORY, this.inputDir);

		// check that each path in the CAS matches a path on disk
		Set<String> pathsSet = new HashSet<String>();
		pathsSet.addAll(Arrays.asList(this.paths));
		for (JCas jCas: new JCasIterable(reader)) {
			String docPath = ViewURIUtil.getURI(jCas).replace('\\', '/');
			Assert.assertTrue(pathsSet.contains(docPath));
			pathsSet.remove(docPath);
		}
		
		reader.close();
		
		// check that all documents were seen
		Assert.assertTrue(pathsSet.isEmpty());
	}

	@Test
	public void testSuffixes() throws IOException, UIMAException {
		
		// create the PlainTextCollectionReader with the HTML input directory  
		CollectionReader reader = CollectionReaderFactory.createCollectionReader(
				FilesCollectionReader.class, null,
				FilesCollectionReader.PARAM_FILE_OR_DIRECTORY, this.inputDir,
				FilesCollectionReader.PARAM_SUFFIXES, new String[] {"1.html"});

		// check that each path in the CAS matches a path on disk
		Set<String> pathsSet = new HashSet<String>();
		pathsSet.addAll(Arrays.asList(this.pathsSuffix1));
		for (JCas jCas: new JCasIterable(reader)) {
			String docPath = ViewURIUtil.getURI(jCas).replace('\\', '/');
			Assert.assertTrue(pathsSet.contains(docPath));
			pathsSet.remove(docPath);
		}
		reader.close();
		
		// check that all documents were seen
		Assert.assertTrue(pathsSet.isEmpty());
	}

	@Test
	public void testSuffixes2() throws IOException, UIMAException {
		
		// create the PlainTextCollectionReader with the HTML input directory  
		CollectionReader reader = CollectionReaderFactory.createCollectionReader(
				FilesCollectionReader.class, null,
				FilesCollectionReader.PARAM_FILE_OR_DIRECTORY, this.inputDir,
				FilesCollectionReader.PARAM_SUFFIXES, new String[] {"1.html", "2.html"});

		// check that each path in the CAS matches a path on disk
		Set<String> pathsSet = new HashSet<String>();
		pathsSet.addAll(Arrays.asList(this.pathsSuffix2));
		for (JCas jCas: new JCasIterable(reader)) {
			String docPath = ViewURIUtil.getURI(jCas).replace('\\', '/');
			Assert.assertTrue(pathsSet.contains(docPath));
			pathsSet.remove(docPath);
		}
		reader.close();
		
		// check that all documents were seen
		Assert.assertTrue(pathsSet.isEmpty());
	}

	private final String[] fileNames = new String[]{
			"11319941.tree",
			"11597317.txt",
			"watcha.txt",
			"huckfinn.txt"};

	@Test
	public void testFileNames() throws IOException, UIMAException {
		
		// create the PlainTextCollectionReader with the HTML input directory  
		CollectionReader reader = CollectionReaderFactory.createCollectionReader(
				FilesCollectionReader.class, null,
				FilesCollectionReader.PARAM_FILE_OR_DIRECTORY, "test/data/docs",
				FilesCollectionReader.PARAM_FILE_NAMES_FILES, new String[] {"test/data/util/PlainTextFileNames.txt"});

		// check that each path in the CAS matches a path on disk
		Set<String> fileNamesSet = new HashSet<String>();
		fileNamesSet.addAll(Arrays.asList(this.fileNames));
		int i=0;
		for (JCas jCas: new JCasIterable(reader)) {
			String fileName = ViewURIUtil.getURI(jCas).replace('\\', '/');
			fileName = fileName.substring(fileName.lastIndexOf("/")+1);
			Assert.assertTrue(fileNamesSet.contains(fileName));
			fileNamesSet.remove(fileName);
			i++;
		}
		
		Assert.assertEquals(4, reader.getProgress()[0].getTotal());

		reader.close();
		
		Assert.assertEquals(4, i);
		// check that all documents were seen
		Assert.assertTrue(fileNamesSet.isEmpty());

	}

	
	/**
	 * Check that the reader works with just a single file.
	 * 
	 * @throws IOException
	 * @throws UIMAException
	 */
	@Test
	public void testSingleFile()  throws IOException, UIMAException {
		String path = "test/data/html/1.html";
		CollectionReader reader = CollectionReaderFactory.createCollectionReader(
				FilesCollectionReader.class, null,
				FilesCollectionReader.PARAM_FILE_OR_DIRECTORY, path);
		
		List<String> paths = new ArrayList<String>();
		for (JCas jCas: new JCasIterable(reader)) {
			paths.add(ViewURIUtil.getURI(jCas).replace('\\', '/'));
		}
		reader.close();
		
		Assert.assertEquals(1, paths.size());
		Assert.assertEquals(path, paths.get(0));
	}
	
	/**
	 * Check that the reader gives an error with an invalid file.
	 * 
	 * @throws IOException
	 * @throws UIMAException
	 */
	@Test
	public void testBadFileException() throws IOException, UIMAException {
		
		try {
			CollectionReaderFactory.createCollectionReader(
					FilesCollectionReader.class, null,
					FilesCollectionReader.PARAM_FILE_OR_DIRECTORY, "data/hmtl");
			Assert.fail("expected error for invalid path");
		} catch (ResourceInitializationException e){}
	}
	
	/**
	 * Get the text of the file referred to by this ClearTK Document.
	 * 
	 * @param jCas The JCas for this document.
	 * @return     The text of the file.
	 * @throws IOException
	 */
	private String getFileText(JCas jCas) throws Exception {
		File docFile = new File(this.inputDir, ViewURIUtil.getURI(jCas));
		return FileUtils.file2String(docFile);
	}
	
	@Test
	public void testDescriptor() throws UIMAException, IOException {
		try {
			CollectionReaderFactory.createCollectionReader("org.cleartk.util.FilesCollectionReader");
			Assert.fail("expected exception with no file or directory specified");
		} catch (ResourceInitializationException e) {}
		
		CollectionReader reader = CollectionReaderFactory.createCollectionReader(
				"org.cleartk.util.FilesCollectionReader",
				FilesCollectionReader.PARAM_FILE_OR_DIRECTORY, this.inputDir);
		
		Object fileOrDirectory = reader.getConfigParameterValue(
				FilesCollectionReader.PARAM_FILE_OR_DIRECTORY);
		Assert.assertEquals(this.inputDir, fileOrDirectory);
		
		Object viewName = reader.getConfigParameterValue(
				FilesCollectionReader.PARAM_VIEW_NAME);
		Assert.assertEquals(null, viewName);
		
		Object encoding = reader.getConfigParameterValue(
				FilesCollectionReader.PARAM_ENCODING);
		Assert.assertEquals(null, encoding);

		Object language = reader.getConfigParameterValue(
				FilesCollectionReader.PARAM_LANGUAGE);
		Assert.assertEquals(null, language);
	}
	
	@Test
	public void testXOR() {
		assertTrue(true ^ false ^ false);
		assertFalse(true ^ true ^ false);
		assertTrue(false ^ false ^ true);
		assertFalse(true ^ false ^ true);
		assertFalse(false ^ false ^ false);
	}
	
}
