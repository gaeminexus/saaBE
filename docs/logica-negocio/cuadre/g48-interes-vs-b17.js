// Interés de la G48 (interesOrdinario, interesMora) contra 1402 y 149905 del B17, por tipo de crédito. Solo lee archivos;
// no imprime identificaciones. Uso: node g48-interes-vs-b17.js <B17.txt> <G48.txt> <etiqueta>
const fs=require('fs');
const [b17,g48,etq]=process.argv.slice(2);
const L=f=>fs.readFileSync(f,'utf8').split(/\r?\n/).filter(Boolean);
const B={};for(const l of L(b17).slice(1)){const c=l.split('\t');B[c[0]]=+c[1];}
const T={};let tot={io:0,im:0,n:0};
for(const l of L(g48).slice(1)){const r=l.split('\t');const t=r[3];T[t]=T[t]||{io:0,im:0,n:0};T[t].io+=+r[10];T[t].im+=+r[11];T[t].n++;tot.io+=+r[10];tot.im+=+r[11];tot.n++;}
const f=x=>(x||0).toLocaleString('es-EC',{minimumFractionDigits:2,maximumFractionDigits:2});
const m={Q:'140205',P:'140210',H:'140215'};
console.log(`${etq}: B17 1402=${f(B['1402'])} 149905=${f(B['149905'])} | G48 interesOrd=${f(tot.io)} interesMora=${f(tot.im)} suma=${f(tot.io+tot.im)} | 1402-(G48 io+im)=${f((B['1402']||0)-tot.io-tot.im)} | 149905+(io+im)=${f((B['149905']||0)+tot.io+tot.im)}`);
for(const t of Object.keys(T).sort())console.log(`   ${t}: B17 ${m[t]}=${f(B[m[t]])}  G48 io=${f(T[t].io)} im=${f(T[t].im)}  dif=${f((B[m[t]]||0)-T[t].io-T[t].im)}`);
