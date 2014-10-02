/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cap.apps;

import java.util.Iterator;
import java.util.LinkedHashMap;
import org.semanticwb.model.WebPage;

/**
 *
 * @author daniel.martinez
 */
public class SWBEmbedPages {
    private static LinkedHashMap<String,String> pages = new LinkedHashMap<String,String>(); 
    
    public static LinkedHashMap<String,String> getPages(WebPage page, String indent){
        getPageChilds(page, indent);                   
        return pages;
    }
    private static void getPageChilds(WebPage page, String indent){
        Iterator<WebPage>  it = page.listVisibleChilds(null);         
        
        if(it.hasNext()){
            while(it.hasNext()) {
                WebPage tp = it.next();                
                if( null != tp ){
                    pages.put(tp.getId(), indent + tp.getDisplayName());
                    if(tp.listVisibleChilds(null).hasNext()){
                        getPageChilds(tp, indent + "-");                      
                    }
                }
            }
        }                                                    
    }       
}
