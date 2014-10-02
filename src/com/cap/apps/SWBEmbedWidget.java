/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cap.apps;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.velocity.VelocityContext;
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
public class SWBEmbedWidget extends GenericAdmResource {
    private static Logger log = SWBUtils.getLogger(SWBEmbedWidget.class);     
    private PrintWriter out = null;

    @Override
    public void doView(HttpServletRequest request, HttpServletResponse response, SWBParamRequest paramsRequest) throws SWBResourceException {
        try {
            VelocityContext context = new VelocityContext();                      
            Resource base = paramsRequest.getResourceBase();
            String idPage = base.getAttribute("sitePage", base.getWebSite().getHomePage().getId());
            WebPage selPage = base.getWebSite().getWebPage(idPage);
            
            Iterator<WebPage> childs = selPage.listChilds("es", true, false, false, true, true);
            List<WebPage> ochilds = new ArrayList<WebPage>();
            while (childs.hasNext())
            {
                WebPage child = childs.next();
                ochilds.add(child);
            }
            
            context.put("childs", ochilds);
            context.put("pageItems", base.getAttribute("pageItems","0"));
            context.put("newWindow", base.getAttribute("newWindow","0"));            
             
            SWBEmbedTemplates.buildTemplate(response, context, "SWBEmbedWidget", base);
            
        } catch (Exception e){
            log.error("Ocurrió un error en la construcción de la vista del recurso:\n "+e.getMessage());
            log.error(SWBEmbed.getStack(e));
            e.printStackTrace();
        }
    }
    
    @Override
    public void doAdmin(HttpServletRequest request, HttpServletResponse response, SWBParamRequest paramReq) {    
        SWBResourceURL url = paramReq.getActionUrl();
        Resource base = getResourceBase();         
        try {            
            VelocityContext context = new VelocityContext();
            WebPage hp = base.getWebSite().getHomePage();            
            LinkedHashMap<String,String> pages = SWBEmbedPages.getPages(hp, "-");            
            context.put("pages", pages);                    
            context.put("actionURL", url);  
            
            context.put("msg", request.getParameter("msg"));
            context.put("sitePage", base.getAttribute("sitePage","0"));
            context.put("pageItems", base.getAttribute("pageItems","0"));
            context.put("newWindow", base.getAttribute("newWindow","0"));
            SWBEmbedTemplates.buildTemplate(response, context, "SWBEmbedWidgetAdmin", base);          
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
        resourceType.setTitle("SWBEmbedWidget");
        resourceType.setDescription("Recurso que inserta un widget para mostrar un listado de contenidos embebidos. ");
        //resourceType.get
        boolean mkDir = false;
        
        try {            
            mkDir = SWBUtils.IO.createDirectory(path);            
        } catch (Exception e){
            log.error("Error intentando crear directorio base o copiando archivos de trabajo para el recurso SWBEmbedWidget ", e);
        }        
        if(mkDir){
            try {            
                JarFile thisJar = SWBEmbedUtils.getJarName(SWBEmbedWidget.class);
                if(thisJar != null){
                    try {
                        SWBEmbedUtils.copyResourcesToDirectory(thisJar, "com/cap/apps/swbembed/assets", path);
                        SWBEmbedUtils.copyResourcesToDirectory(thisJar, "com/cap/apps/swbembedw", path);
                    } catch (IOException e){
                        log.error("Error intentando exportar el directorio assets. ", e);
                    }
                }            
            } catch(Exception e){         
                log.error("Error intentando definir el path del archivo jar de trabajo o exportando directorio de assets. ", e);            
            }
        }
    }      
    
    
}
