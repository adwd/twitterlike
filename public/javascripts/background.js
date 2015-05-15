$(function(){
  $.ajax({
    type : 'GET',
    url : 'https://www.flickr.com/services/rest/',
    data : {
      format : 'json',
      method : 'flickr.photos.search', // 必須 :: 実行メソッド名
      api_key : 'f19913083f594fe98f448414cbd25b84', // 必須 :: API Key
      sort : 'date-posted-desc', // 任意 :: 並べ替え
      tags : 'landscape', // 任意 :: タグで検索
      per_page : '10', // 任意 :: 1回あたりの取得件数
      license: '4' // CC BY 2.0のライセンス
    },
    dataType : 'jsonp',
    jsonp : 'jsoncallback', // Flickrの場合はjsoncallback
    success : _getFlickrPhotos // 通信が成功した場合の処理
  });
});

function _getFlickrPhotos(data){
  var dataStat  = data.stat;
  if(dataStat == 'ok'){
    // success
    var item = data.photos.photo[Math.floor( Math.random() * 10 )];
    var itemFarm = item.farm;
    var itemServer = item.server;
    var itemID = item.id;
    var itemSecret = item.secret;
    var itemTitle = item.title;
    var itemLink = 'https://www.flickr.com/photos/' + item.owner + '/' + itemID;
    var photoPath = 'https://farm' + itemFarm + '.static.flickr.com/' + itemServer + '/' + itemID + '_' + itemSecret + '_b.jpg';
    var footer = $("#flickr-license")[0];
    footer.innerHTML = '<a href="' + itemLink + '">' + itemTitle + '</a>';
    //$("#flickr-license a").attr("href", itemLink);
    $.backstretch(photoPath);
  }else{
    // fail
  }
}