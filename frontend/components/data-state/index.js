Component({
 options:{multipleSlots:true},
 properties:{loading:Boolean,error:String,empty:Boolean,title:{type:String,value:'暂时没有内容'},hint:{type:String,value:'有新内容时会显示在这里。'},actionLabel:String,actionUrl:String,rows:{type:Array,value:[0,1,2]}},
 methods:{retry(){this.triggerEvent('retry')},act(){if(this.data.actionUrl)wx.navigateTo({url:this.data.actionUrl});else this.triggerEvent('action')}}
})
