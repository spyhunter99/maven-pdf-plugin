package org.apache.maven.doxia.docrenderer.pdf.fo;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.maven.doxia.docrenderer.DocumentRendererContext;
import org.apache.maven.doxia.docrenderer.DocumentRendererException;
import org.apache.maven.doxia.docrenderer.pdf.AbstractPdfRenderer;
import org.apache.maven.doxia.docrenderer.pdf.PdfRenderer;
import org.apache.maven.doxia.document.DocumentModel;
import org.apache.maven.doxia.document.DocumentTOC;
import org.apache.maven.doxia.document.DocumentTOCItem;
import org.apache.maven.doxia.module.fo.DocumentStructureExtractionSink;
import org.apache.maven.doxia.module.fo.FoAggregateSink;
import org.apache.maven.doxia.module.fo.FoSink;
import org.apache.maven.doxia.module.fo.FoSinkFactory;
import org.apache.maven.doxia.module.fo.FoUtils;
import org.apache.maven.doxia.parser.module.ParserModule;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;

import org.xml.sax.SAXParseException;

/**
 * PDF renderer that uses Doxia's FO module.
 *
 * @author ltheussl
 * @version $Id: FoPdfRenderer.java 1726406 2016-01-23 15:06:45Z hboutemy $
 * @since 1.1
 */
@Component( role = PdfRenderer.class, hint = "fo" )
public class FoPdfRenderer
    extends AbstractPdfRenderer
{
    /**
     * {@inheritDoc}
     * @see org.apache.maven.doxia.module.fo.FoUtils#convertFO2PDF(File, File, String)
     */
    public void generatePdf( File inputFile, File pdfFile )
        throws DocumentRendererException
    {
        // Should take care of the document model for the metadata...
        generatePdf( inputFile, pdfFile, null );
    }

    /** {@inheritDoc} */
    @Override
    public void render( Map<String, ParserModule> filesToProcess, File outputDirectory, DocumentModel documentModel )
        throws DocumentRendererException, IOException
    {
    	render( filesToProcess, outputDirectory, documentModel, null );
    }

    /** {@inheritDoc} */
    @Override
    public void render( Map<String, ParserModule> filesToProcess, File outputDirectory, DocumentModel documentModel,
                        DocumentRendererContext context )
        throws DocumentRendererException, IOException
    {
    	// copy resources, images, etc.
        copyResources( outputDirectory );
        
        if ( documentModel == null )
        {
            getLogger().debug( "No document model, generating all documents individually." );

            renderIndividual( filesToProcess, outputDirectory, context );
            return;
        }

        String outputName = getOutputName( documentModel );

        File outputFOFile = new File( outputDirectory, outputName + ".fo" );
        if ( !outputFOFile.getParentFile().exists() )
        {
            outputFOFile.getParentFile().mkdirs();
        }

        File pdfOutputFile = new File( outputDirectory, outputName + ".pdf" );
        if ( !pdfOutputFile.getParentFile().exists() )
        {
            pdfOutputFile.getParentFile().mkdirs();
        }

        Writer writer = null;
        String exsumName = null;
        try
        {
            writer = WriterFactory.newXmlWriter( outputFOFile );

            FoAggregateSink sink = new FoAggregateSink( writer, context );

            File fOConfigFile = null;
            if ( context!=null && context.containsKey( "foStylesOverride") )
                fOConfigFile = (File) context.get("foStylesOverride");
            else 
                fOConfigFile = new File( outputDirectory, "pdf-config.xml" );

            if ( fOConfigFile.exists() )
            {
                sink.load( fOConfigFile );
                getLogger().debug( "Loaded pdf config file: " + fOConfigFile.getAbsolutePath() );
            }

            DocumentRendererContext drContext = sink.getRendererContext();
            
            String generateTOC =
                ( context != null && context.get( "generateTOC" ) != null )
                        ? context.get( "generateTOC" ).toString().trim()
                        : "start";
            int tocPosition = 0;
            if ( "start".equalsIgnoreCase( generateTOC ) )
            {
                tocPosition = FoAggregateSink.TOC_START;
            }
            else if ( "end".equalsIgnoreCase( generateTOC ) )
            {
                tocPosition = FoAggregateSink.TOC_END;
            }
            else
            {
                tocPosition = FoAggregateSink.TOC_NONE;
            }
            sink.setDocumentModel( documentModel, tocPosition );

            String hrefExsum = null;
            DocumentTOCItem tocItemExsum = null;
            boolean exsum=false;
            
            scanDocumentsForTOCItems(documentModel, context);
            DocumentTOC toc = documentModel.getToc();
            java.util.Vector<DocumentTOCItem> newList = new java.util.Vector<DocumentTOCItem>();
            
            if( toc!=null )
            {
            	
            	for( DocumentTOCItem item : toc.getItems() )
            	{
            		String href = null;
            		boolean tmpExsum = false;
            		if( item!=null )
            		{
            			
                        if( drContext!=null )
                        {
                        		Object tmp = context.get("executiveSummaryName");
                        	
                        		if( tmp!=null ) exsumName = tmp.toString();//sink.getPomProperty("pdf.executivesummaryname");
                        }
                        if( item.getName() != null && item.getName().trim().equalsIgnoreCase(exsumName)  )
            			{
            				newList.add(0, item);
            				exsum = true;
            				tmpExsum = true;
            				hrefExsum = href;
            				tocItemExsum = item;
            			}
                        else 
            			{
            				newList.addElement(item);
            			}
            			
            			href = StringUtils.replace( item.getRef(), "\\", "/" );
        	            if ( href!=null && href.lastIndexOf( '.' ) != -1 )
        	            {
        	                href = href.substring( 0, href.lastIndexOf( '.' ) );
        	            }
        	            if( tmpExsum )
        	            {
        	            	hrefExsum = href;
        	            }
            		}
            		
    	            
            	}
            	toc.setItems(newList);
            }

            sink.beginDocument();
            
            //removing TOC and ExSum from the TOC has to be here, to make sure both are still in the bookmarks
            DocumentTOCItem dti = null;
            for( int i=newList.size()-1; i>=0 ; i-- )
            {
            	dti= newList.elementAt(i);
            	if( dti!=null )
            	{
            		if( dti.getName().equals(exsumName) ||
            				dti.getRef().equals("./toc") )
            			newList.remove(i);
            	}
            }
            if (toc!=null)
            toc.setItems(newList);
            
            sink.coverPage();
            
            if( exsum )
            {
            	sink.activatePriorPageWriting(true);
            	sink.setDocumentTitle(tocItemExsum.getName());
            	sink.setDocumentName(tocItemExsum.getRef());
            	sink.execSum(documentModel, exsumName);
            	renderModules( hrefExsum, sink, tocItemExsum, context );
            	sink.activatePriorPageWriting(false);
            	sink.resetPageCounter();
            }
            
            if ( tocPosition == FoAggregateSink.TOC_START )
            {
                sink.toc();
            }
            sink.resetPageCounter();
            sink.setChapter(0);
            if ( ( documentModel.getToc() == null ) || ( documentModel.getToc().getItems() == null ) )
            {
                getLogger().info( "No TOC is defined in the document descriptor. Merging all documents." );

                mergeAllSources( filesToProcess, sink, context );
            }
            else
            {
                getLogger().debug( "Using TOC defined in the document descriptor." );

                mergeSourcesFromTOC( documentModel.getToc(), sink, context );
            }

            if ( tocPosition == FoAggregateSink.TOC_END )
            {
                sink.toc();
            }

            sink.endDocument();
        }
        finally
        {
            IOUtil.close( writer );
        }

        generatePdf( outputFOFile, pdfOutputFile, documentModel );
    }

    /**
     * Scans the source-documents for their inner structure to identify (sub-)chapters in order to complete the table of contents. 
     * @param dModel the DoumentModel, not null
     * @param context the renderer-context
     */
    private void scanDocumentsForTOCItems(DocumentModel dModel, DocumentRendererContext context)
    {
    	if( dModel!=null )
    	{
	    	DocumentTOC toc = dModel.getToc();
	    	if( toc!=null )
	    	{
	    		DocumentStructureExtractionSink dsSink = new DocumentStructureExtractionSink(new DummyWriter());

	    		for( DocumentTOCItem tocItem: toc.getItems() )
	    		{
		    		try {
		    			parseTOCItem( tocItem, dsSink, context );
						dsSink.enrichTOCItemWithSubstructure(tocItem);
						dsSink.reset();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (DocumentRendererException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    		}
	    	}
    	}
    }
    
    /** {@inheritDoc} */
    @Override
    public void renderIndividual( Map<String, ParserModule> filesToProcess, File outputDirectory )
        throws DocumentRendererException, IOException
    {
        renderIndividual( filesToProcess, outputDirectory, null );
    }

    /** {@inheritDoc} */
    @Override
    public void renderIndividual( Map<String, ParserModule> filesToProcess, File outputDirectory,
                                  DocumentRendererContext context )
        throws DocumentRendererException, IOException
    {
        for ( Map.Entry<String, ParserModule> entry : filesToProcess.entrySet() )
        {
            String key = entry.getKey();
            ParserModule module = entry.getValue();

            File fullDoc = new File( getBaseDir(), module.getSourceDirectory() + File.separator + key );

            String output = key;
            for ( String extension : module.getExtensions() )
            {
                String lowerCaseExtension = extension.toLowerCase( Locale.ENGLISH );
                if ( output.toLowerCase( Locale.ENGLISH ).indexOf( "." + lowerCaseExtension ) != -1 )
                {
                    output =
                        output.substring( 0, output.toLowerCase( Locale.ENGLISH ).indexOf( "." + lowerCaseExtension ) );
                }
            }

            File outputFOFile = new File( outputDirectory, output + ".fo" );
            if ( !outputFOFile.getParentFile().exists() )
            {
                outputFOFile.getParentFile().mkdirs();
            }

            File pdfOutputFile = new File( outputDirectory, output + ".pdf" );
            if ( !pdfOutputFile.getParentFile().exists() )
            {
                pdfOutputFile.getParentFile().mkdirs();
            }

            FoSink sink =
                (FoSink) new FoSinkFactory().createSink( outputFOFile.getParentFile(), outputFOFile.getName() );
            sink.beginDocument();
            parse( fullDoc.getAbsolutePath(), module.getParserId(), sink, context );
            sink.endDocument();

            generatePdf( outputFOFile, pdfOutputFile, null );
        }
    }

    private void mergeAllSources( Map<String, ParserModule> filesToProcess, FoAggregateSink sink,
                                  DocumentRendererContext context )
        throws DocumentRendererException, IOException
    {
        for ( Map.Entry<String, ParserModule> entry : filesToProcess.entrySet() )
        {
            String key = entry.getKey();
            ParserModule module = entry.getValue();
            sink.setDocumentName( key );
            File fullDoc = new File( getBaseDir(), module.getSourceDirectory() + File.separator + key );

            parse( fullDoc.getAbsolutePath(), module.getParserId(), sink, context );
        }
    }

    private void mergeSourcesFromTOC( DocumentTOC toc, FoAggregateSink sink, DocumentRendererContext context )
        throws IOException, DocumentRendererException
    {
        parseTocItems( toc.getItems(), sink, context );
    }

    private void parseTocItems( List<DocumentTOCItem> items, FoAggregateSink sink, DocumentRendererContext context )
        throws IOException, DocumentRendererException
    {
        for( DocumentTOCItem tocItem : items )
        {
        	if( tocItem!=null )
        	{
        		parseTOCItem(tocItem, sink, context);
        	}
        }
    }

    private void parseTOCItem(DocumentTOCItem tocItem, FoAggregateSink sink, DocumentRendererContext context ) throws IOException, DocumentRendererException
    {
    	if( tocItem!=null && sink!=null && context !=null )
    	{
    		if( tocItem!=null )
        	{
	            if ( tocItem.getRef() == null )
	            {
	                if ( getLogger().isInfoEnabled() )
	                {
	                    getLogger().info( "No ref defined for tocItem " + tocItem.getName() );
	                }
	
	                return;
	            }
	            
	            String exsumName = null;
	            if( context!=null )
	            {
	            		Object tmp = context.get("executiveSummaryName");
	            	
	            		if( tmp!=null ) exsumName = tmp.toString();
	            }
	
                    if (tocItem.getName()==null) {
                        throw new DocumentRendererException("Unable to obtain a ToC item text from the document at " + 
                                tocItem.getRef() + ". This could be because of a type "
                                        + "in the pdf.xml descriptor, missing the 'name' attribute?");
                    }
	            if( (exsumName!=null && tocItem.getName()!=null && !tocItem.getName().trim().equalsIgnoreCase(exsumName)) || exsumName==null )
	            {
		            String href = StringUtils.replace( tocItem.getRef(), "\\", "/" );
		            if ( href.lastIndexOf( '.' ) != -1 )
		            {
		                href = href.substring( 0, href.lastIndexOf( '.' ) );
		            }
		
		            renderModules( href, sink, tocItem, context );
		
		            if ( tocItem.getItems() != null )
		            {
		                parseTocItems( tocItem.getItems(), sink, context );
		            }
	            }
        	}
    	}
    }
    
    private void renderModules( String href, FoAggregateSink sink, DocumentTOCItem tocItem,
                                DocumentRendererContext context )
        throws DocumentRendererException, IOException
    {
        Collection<ParserModule> modules = parserModuleManager.getParserModules();
        for ( ParserModule module : modules )
        {
            File moduleBasedir = new File( getBaseDir(), module.getSourceDirectory() );

            if ( moduleBasedir.exists() )
            {
                for ( String extension : module.getExtensions() )
                {
                    String doc = href + "." + extension;
                    File source = new File( moduleBasedir, doc );
    
                    // Velocity file?
                    if ( !source.exists() )
                    {
                        if ( href.indexOf( "." + extension ) != -1 )
                        {
                            doc = href + ".vm";
                        }
                        else
                        {
                            doc = href + "." + extension + ".vm";
                        }
                        source = new File( moduleBasedir, doc );
                    }

                    if ( source.exists() )
                    {
                    	sink.setDocumentName( doc );
                        sink.setDocumentTitle( tocItem.getName() );

                        parse( source.getPath(), module.getParserId(), sink, context );
                    }
                }
            }
        }
    }

    /**
     * @param inputFile
     * @param pdfFile
     * @param documentModel could be null
     * @throws DocumentRendererException if any
     * @since 1.1.1
     */
    private void generatePdf( File inputFile, File pdfFile, DocumentModel documentModel )
        throws DocumentRendererException
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Generating: " + pdfFile );
        }

        try
        {
            FoUtils.convertFO2PDF( inputFile, pdfFile, null, documentModel );
        }
        catch ( TransformerException e )
        {
            if ( ( e.getCause() != null ) && ( e.getCause() instanceof SAXParseException ) )
            {
                SAXParseException sax = (SAXParseException) e.getCause();

                StringBuilder sb = new StringBuilder();
                sb.append( "Error creating PDF from " ).append( inputFile.getAbsolutePath() ).append( ":" )
                  .append( sax.getLineNumber() ).append( ":" ).append( sax.getColumnNumber() ).append( "\n" );
                sb.append( e.getMessage() );

                throw new DocumentRendererException( sb.toString() );
            }

            throw new DocumentRendererException( "Error creating PDF from " + inputFile + ": " + e.getMessage() );
        }
    }
    
    /**
     * Neccessary to make parsing to DocumentStructureExtractionSink (in order to extract the TOC-Items from the documents) 
     * possible without harming existing outputs 
     */
    private class DummyWriter extends java.io.Writer
	{

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void flush() throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void close() throws IOException {
			// TODO Auto-generated method stub
			
		}
		
	}
}
