package com.bdqn.mall.controller.admin;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bdqn.mall.controller.BaseController;
import com.bdqn.mall.entity.*;
import com.bdqn.mall.service.*;
import com.bdqn.mall.util.OrderUtil;
import com.bdqn.mall.util.PageUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

@Controller
public class AdminProductController extends BaseController {
    @Resource
    private CategoryService categoryService;
    @Resource
    private ProductService productService;
    @Resource
    private UserService userService;
    @Resource
    private ProductOrderService productOrderService;
    @Resource
    private ProductImageService productImageService;
    @Resource
    private PropertyValueService propertyValueService;
    @Resource
    private PropertyService propertyService;
    @Resource
    private LastIDService lastIDService;
    //所有产品信息
    @RequestMapping(value = "admin/product",method = RequestMethod.GET)
    public String goToPage(HttpSession session, Map<String,Object> map){
        //检查管理员权限
        Object adminId=checkAdmin(session);
        if (adminId == null){
            return "admin/include/loginMessage";
        }
        //获取分类
        List<Category> categoryList=categoryService.getcategorylist(null,null);
        map.put("categoryList",categoryList);
        //获取产品
        PageUtil pageUtil=new PageUtil(0,10);
        List<Product> productList=productService.getList(null,null,null,pageUtil);
        map.put("productList",productList);
        //获取产品总数量
        Integer productCount=productService.getTotal(null,null);
        map.put("productCount",productCount);
        //获取分页信息
        pageUtil.setTotal(productCount);
        return "admin/productManagePage";
    }
    //按照条件查询产品信息ajax\
    @ResponseBody
    @RequestMapping(value = "admin/product/{index}/{count}",method = RequestMethod.GET,produces = "application/json;charset=utf-8")
    public String getProductBySearch(@RequestParam(required = false) String productName/* 产品名称 */,
                                     @RequestParam(required = false) Integer categoryId/* 产品类型ID */,
                                     @RequestParam(required = false) Double productSalePrice/* 产品最低价 */,
                                     @RequestParam(required = false) Double productPrice /* 产品最高价 */,
                                     @RequestParam(required = false) Byte[] productIsEnabledArray /* 产品状态数组 */,
                                     @RequestParam(required = false) String orderBy/* 排序字段 */,
                                     @RequestParam(required = false,defaultValue = "true") Boolean isDesc/* 是否倒序*/,
                                     @PathVariable Integer index /* 页数 */,
                                     @PathVariable Integer count /* 行数 */) throws UnsupportedEncodingException {
        //移除不必要条件
        if (productIsEnabledArray != null && (productIsEnabledArray.length <=0 || productIsEnabledArray.length>0)){
            productIsEnabledArray = null;
        }
        if (categoryId != null && categoryId ==0){
            categoryId = null;
        }
        if (productName != null){
            //如果为非空字符串则解决中文乱码：URLDecoder.decode(String,"UTF-8");
            productName = productName.equals("") ? null : URLDecoder.decode(productName,"UTF-8");
        }
        if (orderBy != null && orderBy.equals("")){
            orderBy = null;
        }
        //封装查询条件
        Product product= new Product();
        Category category = new Category();
        category.setCategoryId(categoryId);
        product.setProductName(productName);
        product.setProductCategory(category);
        product.setProductPrice(productPrice);
        OrderUtil orderUtil = null;
        if (orderBy != null){
            logger.info("根据{}排序，是否倒序：{}",orderBy,isDesc);
        }
        JSONObject object= new JSONObject();
        logger.info("按条件获取第{}页的{}条产品",index+1,count);
        PageUtil pageUtil = new PageUtil(index,count);
        List<Product> productList=productService.getList(product,productIsEnabledArray,orderUtil,pageUtil);
        object.put("productList", JSONArray.parseArray(JSON.toJSONString(productList)));
        //按条件获取产品总数量
        Integer productCount=productService.getTotal(product,productIsEnabledArray);
        object.put("productCount",productCount);
        //获取分页信息
        pageUtil.setTotal(productCount);
        object.put("totalPage",pageUtil.getTotalPage());
        object.put("pageUtil",pageUtil);
        return object.toJSONString();
    }
    //转到后台管理-产品详情页-ajax
    @RequestMapping(value = "admin/product/{pid}",method = RequestMethod.GET)
    public String goToDetailsPage(HttpSession session,Map<String,Object>map,@PathVariable Integer pid/* 产品id */){
        //检查管理员权限
        Object adminId= checkAdmin(session);
        if (adminId == null){
            return "admin/include/loginMessage";
        }
        logger.info("获取product_id为{}的产品信息",pid);
        Product product = productService.get(pid);
        //获取产品详情-图片信息
        Integer productId = product.getProductId();
        List<ProductImage> productImageList =productImageService.getList(productId,null,null);
        List<ProductImage> singleProductImageList =new ArrayList<>(5);
        List<ProductImage> detailsProductImageList = new ArrayList<>(8);
        for (ProductImage productImage : productImageList){
            if (productImage.getProductImageType() ==0){
                singleProductImageList.add(productImage);
            }else {
                detailsProductImageList.add(productImage);
            }
        }
        product.setSingleProductImageList(singleProductImageList);
        product.setDetailProductImageList(detailsProductImageList);
        map.put("product",product);
        //获取产品详情-属性值信息
        PropertyValue propertyValue = new PropertyValue();
        propertyValue.setPropertyValueProduct(product);
        //根据商品的id去查询属性值表的数据
        List<PropertyValue> propertyValueList=propertyValueService.getList(propertyValue,null);
        //获取产品详情-分类信息对应的属性列表
        Property property =new Property();
        property.setPropertyCategory(product.getProductCategory());
        //通过分类id去查询属性表的数据
        List<Property> propertyList = propertyService.getList(property,null);
        //属性列表和属性值列表合并
        for (Property properyEach : propertyList){
            for (PropertyValue properyValueEach : propertyValueList){
                if (properyEach.getPropertyId().equals(properyValueEach.getPropertyValueProperty().getPropertyId())){
                    List<PropertyValue> propertyValueItem = new ArrayList<>(1);
                    propertyValueItem.add(properyValueEach);
                    properyEach.setPropertyValueList(propertyValueItem);
                    break;
                }
            }
        }
        map.put("propertyList",propertyList);
        //获取分类列表
        List<Category> categoryList = categoryService.getcategorylist(null,null);
        map.put("categoryList",categoryList);
        //转到后台管理-产品详情页-ajax方式
        return "admin/include/productDetails";
    }
    //按id删除产品图片并返回最新结果-ajax
    @ResponseBody
    @RequestMapping(value = "admin/productImage/{productImageId}",method = RequestMethod.DELETE,produces = "application/json;charset=utf-8")
    public String deleteProductImageById(@PathVariable Integer productImageId/* 产品图片id */){
        JSONObject object= new JSONObject();
        //获取productImageId为{}的产品图片信息
        //ProductImage productImage = productImageService.
        //删除产品图片
        Boolean yn= productImageService.deleteList(new Integer[]{productImageId});
        if (yn){
            //删除图片成功
            object.put("success",true);
        }else {
            //删除图片失败！事务回滚
            object.put("success",false);
            throw new RuntimeException();
        }
        return object.toJSONString();
    }
    //更新产品信息-ajax
    @ResponseBody
    @RequestMapping(value = "admin/product/{productId}",method = RequestMethod.PUT,produces = "application/json;charset=utf-8")
    public String updateProduct(@RequestParam String productName/* 产品名称 */,
                                @RequestParam String productTitle/* 产品标题 */,
                                @RequestParam Integer productCategoryId/* 产品类型ID */,
                                @RequestParam Double productSalePrice/* 产品最低价 */,
                                @RequestParam Double productPrice/* 产品最高价 */,
                                @RequestParam Byte productIsEnabled/* 产品状态 */,
                                @RequestParam String propertyAddJson/* 产品添加属性JSON */,
                                @RequestParam String propertyUpdateJson/* 产品更新属性JSON */,
                                @RequestParam(required = false) Integer[] propertyDeleteList/* 产品删除属性Id数组 */,
                                @RequestParam(required = false) String[] productSingleImageList/* 产品预览图片名称数组 */,
                                @RequestParam(required = false) String[] productDetailsImageList/* 产品详情图片名称数组 */,
                                @PathVariable("productId")Integer productId/* 产品ID */){
        JSONObject jsonObject = new JSONObject();
        logger.info("整合产品信息");
        Category category = new Category();
        category.setCategoryId(productCategoryId);
        Product product = new Product(productId,productName,productTitle,category,productSalePrice,productPrice,productIsEnabled,new Date());
        logger.info("更新产品信息,产品ID值为：{}",productId);
        Boolean yn = productService.update(product);
        if (!yn){
            logger.info("产品信息更新失败，事务回滚");
            jsonObject.put("success",false);
            throw new RuntimeException();
        }
        logger.info("产品信息更新成功");

        JSONObject object = JSON.parseObject(propertyAddJson);
        //取出所有的键名
        Set<String> propertyIdSet = object.keySet();
        if (propertyIdSet.size() > 0 ){
            logger.info("整合产品子信息-需要添加的产品属性");
            List<PropertyValue> propertyValueList = new ArrayList<>(5);
            for (String key : propertyIdSet){
                Property property = new Property();
                //设置属性的id（key就是每一个属性的id）
                property.setPropertyId(Integer.valueOf(key));
                //通过键名取出属性值
                String value = object.getString(key.toString());

                PropertyValue propertyValue = new PropertyValue(value,property,product);
                propertyValueList.add(propertyValue);
            }
            logger.info("共有{}条需要添加的产品属性数据",propertyValueList.size());
            yn = propertyValueService.addList(propertyValueList);
            if (yn){
                logger.info("产品属性添加成功");
            }else {
                logger.warn("产品属性添加失败，事务回滚");
                jsonObject.put("success",false);
                throw  new RuntimeException();
            }
        }
        object = JSON.parseObject(propertyUpdateJson);
        propertyIdSet = object.keySet();
        if (propertyIdSet.size() > 0 ){
            logger.info("整合产品子信息-需要更新的产品属性数据");
            List<PropertyValue> propertyValueList = new ArrayList<>(5);
            for (String key : propertyIdSet){
                //通过键名取出属性值
                String value = object.getString(key.toString());
                PropertyValue propertyValue = new PropertyValue(value,Integer.valueOf(key));
                propertyValueList.add(propertyValue);
            }
            logger.info("共有{}条需要更新的产品属性数据",propertyValueList.size());
            for (int i = 0 ; i < propertyValueList.size() ; i++ ){
                logger.info("正在更新第{}条，共{}条",i+1,propertyValueList.size());
                yn = propertyValueService.update(propertyValueList.get(i));
                if (yn){
                    logger.info("产品属性更新成功");
                }else {
                    logger.warn("产品属性更新失败！事务回滚");
                    jsonObject.put("success",false);
                    throw new RuntimeException();
                }
            }
        }
        if (propertyDeleteList != null && propertyDeleteList.length > 0){
            logger.info("整合产品子信息-需要删除的产品属性");
            logger.info("共有{}条需要删除的产品属性数据",propertyDeleteList.length);
            yn = propertyValueService.deleteList(propertyDeleteList);
            if (yn){
                logger.info("产品属性删除成功");
            }else {
                logger.warn("产品属性删除失败!事务回滚");
                jsonObject.put("success",false);
                throw new RuntimeException();
            }
        }
        if (productSingleImageList != null && productSingleImageList.length > 0){
            logger.info("整合产品子信息-产品预览图片");
            List<ProductImage> productImageList = new ArrayList<>(5);
            for (String imageName : productSingleImageList){
                ProductImage productImage = new ProductImage();
                productImage.setProductImageSrc(imageName.substring(imageName.lastIndexOf("/")+1));
                productImage.setProductImageType((byte) 0);
                productImage.setProductImageProduct(product);
                productImageList.add(productImage);
            }
            logger.info("共有{}条产品预览图片数据",productImageList.size());
            yn = productImageService.addList(productImageList);
            if (yn){
                logger.info("产品预览图片添加成功");
            }else {
                logger.warn("产品预览图片添加失败！事务回滚");
                jsonObject.put("success",false);
                throw new RuntimeException();
            }
        }
        if (productDetailsImageList != null && productDetailsImageList.length > 0){
            logger.info("整合产品子信息-产品详情图片");
            List<ProductImage> productImageList = new ArrayList<>(5);
            for (String imageName : productDetailsImageList){
                ProductImage productImage = new ProductImage();
                productImage.setProductImageType((byte) 0);
                productImage.setProductImageProduct(product);
                productImage.setProductImageSrc(imageName.substring(imageName.lastIndexOf("/")+1));
                productImageList.add(productImage);
            }
            logger.info("共有{}条产品详情图片数据",productImageList.size());
            yn = productImageService.addList(productImageList);
            if (yn){
                logger.info("产品详情图片添加成功！");
            }else {
                logger.warn("产品详情图片添加失败！事务回滚");
                jsonObject.put("success",false);
                throw new RuntimeException();
            }
        }
        jsonObject.put("success",true);
        jsonObject.put("productId",productId);

        return jsonObject.toJSONString();
    }
    //添加产品信息-ajax
    @ResponseBody
    @RequestMapping(value = "admin/product",method = RequestMethod.POST,produces = "application/json;charset=utf-8")
    public String addProduct(@RequestParam String productName/* 产品名称 */,
                             @RequestParam String productTitle/* 产品标题 */,
                             @RequestParam Integer productCategoryId/* 产品类型ID */,
                             @RequestParam Double productSalePrice/* 产品最低价 */,
                             @RequestParam Double productPrice/* 产品最高价 */,
                             @RequestParam Byte productIsEnabled/* 产品状态 */,
                             @RequestParam String propertyJson/* 产品属性JSON */,
                             @RequestParam(required = false) String[] productSingleImageList/* 产品预览图片名称数组 */,
                             @RequestParam(required = false) String[] productDetailsImageList/* 产品详情图片名称数组 */){
        JSONObject jsonObject= new JSONObject();
        //整合产品信息
        Category category= new Category();
        category.setCategoryId(productCategoryId);
        Product product = new Product(productName,productTitle,category,productSalePrice,productPrice,productIsEnabled,new Date());
        //添加产品信息
        Boolean yn= productService.add(product);
        if (!yn){
            //产品添加失败！事务回滚
            jsonObject.put("success",false);
            throw new RuntimeException();
        }
        int productId = lastIDService.selectLastID();
        logger.info("添加成功，新增产品的id值为{}",productId);
        Product productNew = new Product();
        productNew.setProductId(productId);
        JSONObject object= JSON.parseObject(propertyJson);
        Set<String> propertyIdSet = object.keySet();
        if (propertyIdSet.size() > 0 ){
            //整合产品子信息-需要添加的产品属性
            List<PropertyValue> propertyValueList = new ArrayList<>(5);
            for (String key : propertyIdSet){
                Property property = new Property();
                //设置属性的id（key就是每一个属性的id）
                property.setPropertyId(Integer.valueOf(key));
                //通过键名取出属性值
                String value = object.getString(key.toString());

                PropertyValue propertyValue = new PropertyValue(value,property,productNew);
                propertyValueList.add(propertyValue);
            }
            logger.info("共有{}条需要添加的产品属性数据",propertyValueList.size());
            yn = propertyValueService.addList(propertyValueList);
            if (yn){
                logger.info("产品属性添加成功");
            }else {
                logger.warn("产品属性添加失败，事务回滚");
                jsonObject.put("success",false);
                throw  new RuntimeException();
            }
        }
        if (productSingleImageList != null && productSingleImageList.length > 0){
            //整合产品子信息-产品预览图片
            List<ProductImage> productImageList = new ArrayList<>(5);
            for (String imageName : productSingleImageList){
                Product product1 = new Product();
                product1.setProductId(productId);
                productImageList.add(new ProductImage((byte) 0,imageName.substring(imageName.lastIndexOf("/")+1),product1));
            }
            logger.info("共有{}条产品预览图片数据",productImageList.size());
            yn = productImageService.addList(productImageList);
            if (yn){
                logger.info("产品预览图片添加成功");
            }else {
                logger.warn("产品预览图片添加失败！事务回滚");
                jsonObject.put("success",false);
                throw new RuntimeException();
            }
        }
        if (productDetailsImageList != null && productDetailsImageList.length > 0){
            logger.info("整合产品子信息-产品详情图片");
            List<ProductImage> productImageList = new ArrayList<>(5);
            for (String imageName : productDetailsImageList){
                Product product1 = new Product();
                product1.setProductId(productId);
                productImageList.add(new ProductImage((byte) 1,imageName.substring(imageName.lastIndexOf("/")+1),product1));
            }
            logger.info("共有{}条产品详情图片数据",productImageList.size());
            yn = productImageService.addList(productImageList);
            if (yn){
                logger.info("产品详情图片添加成功！");
            }else {
                logger.warn("产品详情图片添加失败！事务回滚");
                jsonObject.put("success",false);
                throw new RuntimeException();
            }
        }
        jsonObject.put("success",true);
        jsonObject.put("productId",productId);

        return jsonObject.toJSONString();
    }

}
