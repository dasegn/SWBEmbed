/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cap.apps;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.semanticwb.Logger;
import org.semanticwb.SWBException;
import org.semanticwb.SWBPortal;
import org.semanticwb.SWBUtils;
import org.semanticwb.model.Resource;
import org.semanticwb.model.ResourceType;
import org.semanticwb.model.WebPage;
import org.semanticwb.portal.api.GenericAdmResource;
import org.semanticwb.portal.api.SWBActionResponse;
import org.semanticwb.portal.api.SWBParamRequest;
import org.semanticwb.portal.api.SWBResourceException;
import org.semanticwb.portal.api.SWBResourceURL;


/**
 *
 * @author daniel.martinez
 */
public class SWBEmbed extends GenericAdmResource {
    private static Logger log = SWBUtils.getLogger(SWBEmbed.class);     
    private PrintWriter out = null;  
    
    @Override
    public void doView(HttpServletRequest request, HttpServletResponse response, SWBParamRequest paramsRequest) throws SWBResourceException {
        try {
            VelocityContext context = new VelocityContext();           
            Resource base = paramsRequest.getResourceBase();
            WebPage current = paramsRequest.getWebPage();            
            Iterator<WebPage> childs = current.listChilds("es", true, false, false, true, true);
            List<WebPage> ochilds = new ArrayList<WebPage>();
            while (childs.hasNext())
            {
                WebPage child = childs.next();
                ochilds.add(child);
            }

            context.put("childs", ochilds);
            context.put("tmpl", base.getAttribute("tmpl", ""));
            context.put("idSlide", base.getAttribute("idSlide", ""));
            SWBEmbedTemplates.buildTemplate(response, context, "SWBEmbed", base);
            
        } catch (Exception e){
            log.error("Ocurrió un error en la construcción de la vista del rescurso:\n "+e.getMessage());
            e.printStackTrace();
        }
    }
    @Override
    public void doAdmin(HttpServletRequest request, HttpServletResponse response, SWBParamRequest paramReq) {    
        SWBResourceURL url = paramReq.getActionUrl();        
        try {            
            VelocityContext context = new VelocityContext();
            Resource base = paramReq.getResourceBase(); 
            context.put("tmpl", base.getAttribute("tmpl", ""));
            context.put("actionURL", url);            
            context.put("msg", request.getParameter("msg"));
            context.put("idSlide", base.getAttribute("idSlide",""));      
            SWBEmbedTemplates.buildTemplate(response, context, "SWBEmbedAdmin", base);
        } catch(Exception e){
            log.error("Ocurrió un error durante la construcción de la vista de administración. "+e.getMessage()); 
            e.printStackTrace();
        }        
    }

    @Override
    public void processAction(HttpServletRequest request, SWBActionResponse response) throws SWBResourceException, IOException {
        Resource base = getResourceBase();
        try {
            Enumeration names = request.getParameterNames();
            while (names.hasMoreElements()){
                String name = (String) names.nextElement();
                base.setAttribute(name, request.getParameter(name));
            }
            base.updateAttributesToDB();
            response.setRenderParameter("msg", "true");            
        } catch(SWBException e){
            response.setRenderParameter("msg", "false");            
            log.error(e);
        }
    }    

    
    @Override
    public void install(ResourceType resourceType) throws SWBResourceException {  
        String path = SWBPortal.getWorkPath()+resourceType.getWorkPath();

        // Estableciendo parametros de la instancia
        resourceType.setTitle("SWBEmbed");
        resourceType.setDescription("Recurso que inserta medios de distintas fuentes (Slideshare). ");
        //resourceType.get
        boolean mkDir = false;
        
        try {            
            mkDir = SWBUtils.IO.createDirectory(path);            
        } catch (Exception e){
            log.error("Error intentando crear directorio base o copiando archivos de trabajo para el recurso SWBFeed ", e);
        }        
        if(mkDir){
            try {            
                JarFile thisJar = SWBEmbedUtils.getJarName(SWBEmbed.class);
                if(thisJar != null){
                    try {
                        SWBEmbedUtils.copyResourcesToDirectory(thisJar, "com/cap/apps/swbembed/assets", path);
                    } catch (IOException e){
                        log.error("Error intentando exportar el directorio assets. ", e);
                    }
                }            
            } catch(Exception e){         
                log.error("Error intentando definir el path del archivo jar de trabajo o exportando directorio de assets. ", e);            
            }
        }
    }
    
   @Override
    public void uninstall(ResourceType resourceType) throws SWBResourceException {
        String path = SWBPortal.getWorkPath() + resourceType.getWorkPath();
        try {
            boolean deleteDirectory = SWBUtils.IO.removeDirectory(path);
        } catch (Exception e){
            log.error("Error intentando eliminar directorio de trabajo para el recurso. ", e);
        }    
   }  
   
    @Override
    public void setResourceBase(Resource base) {
        try {                                          
            super.setResourceBase(base);
            Iterator<String> it = base.getAttributeNames();
            
            while(it.hasNext()) {
                String attname = it.next();
                String attval = base.getAttribute(attname);                
                if(attname.startsWith("path") && attval != null){
                    attval = attval.replaceAll("(\\r|\\n)","");
                    base.setAttribute(attname, attval);
                }                
            }
            try {
                base.updateAttributesToDB();
            } catch(Exception e) {
                log.error(e);
            }                       
        } catch(Exception e) { 
            log.error("Error while setting resource base: "+base.getId() +"-"+ base.getTitle(), e);
        }
    }
    

    protected static String getStack(Exception e){
        StringBuilder stck = new StringBuilder();
        stck.append("Mensaje: "+e.getMessage()+"\n");
        StackTraceElement[] trace = e.getStackTrace();
        for (StackTraceElement element : trace) {
          stck.append("----------------------------------\n");
          stck.append("Clase: ").append(element.getClassName()).append("\n");
          stck.append("Metodo: ").append(element.getMethodName()).append("\n");
          stck.append("Archivo: ").append(element.getFileName()).append("\n");
          stck.append("Linea: ").append(element.getLineNumber()).append("\n");
          stck.append("----------------------------------");          
        }
        return stck.toString();
    }     
}
