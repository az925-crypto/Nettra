package com.zaaam.nettra.webview

object FingerprintInjector {
    fun script(level: String): String = when(level) {
        "Strict" -> """
            (function(){
                try{
                    HTMLCanvasElement.prototype.getContext = new Proxy(HTMLCanvasElement.prototype.getContext, {apply(t,th,a){const c=Reflect.apply(t,th,a); if(a[0]==='2d' && c){ const orig=c.getImageData; c.getImageData=function(){const d=orig.apply(this,arguments); for(let i=0;i<d.data.length;i++) d.data[i]+= Math.floor(Math.random()*10)-5; return d;}} return c;}});
                    Object.defineProperty(navigator,'plugins',{get:()=>[]}); Object.defineProperty(navigator,'hardwareConcurrency',{get:()=>4});
                    const AC=window.AudioContext||window.webkitAudioContext; if(AC){ const p=AC.prototype.createOscillator; AC.prototype.createOscillator=function(){const o=p.apply(this,arguments); const od=o.connect; return o;} }
                }catch(e){}
            })();
        """.trimIndent()
        "Balanced" -> """
            (function(){
                try{
                    const c=HTMLCanvasElement.prototype.getContext; HTMLCanvasElement.prototype.getContext=function(t){const ctx=c.call(this,t); if(t==='2d'&&ctx){const o=ctx.getImageData; ctx.getImageData=function(){const d=o.apply(this,arguments); for(let i=0;i<d.data.length;i+=4) d.data[i]^=1; return d;}} return ctx;}
                }catch(e){}
            })();
        """.trimIndent()
        else -> ""
    }
}
