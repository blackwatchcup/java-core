
// put 关键函数
final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        //缓存 tab  p n大小 i
        Node<K,V>[] tab; Node<K,V> p; int n, i;
        //步骤①：tab为空则创建
        //数组table是否为空 扩容resize
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;
        // 步骤②：计算index，并对null做处理
        // (n - 1) & hash 确定元素存放在哪个桶中，桶为空，新生成结点放入桶中
        //(此时，这个结点是放在数组中)
        // 求余方式前提要保证 n 为2的幂次方 这样就可以通过与运算求余
        //即(n-1) & hash == hash / n 除法 n << 3 == n/8
        //为null直接插入
        if ((p = tab[i = (n - 1) & hash]) == null)
            tab[i] = newNode(hash, key, value, null);
        //值存在
        else {
            Node<K,V> e; K k;
            //key 相同直接覆盖
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                // e暂时储存p
                e = p;
            //如果是treenode存在，直接
            else if (p instanceof TreeNode)
            //放入树中
                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
            //为链表
            else {
              //尾插
                for (int binCount = 0; ; ++binCount) {
                  //是否为最后一个节点
                    if ((e = p.next) == null) {
                      //插入
                        p.next = newNode(hash, key, value, null);
                        //如果大于阈值直接转为红黑树
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            treeifyBin(tab, hash);
                        //跳出
                        break;
                    }
                    //key 与 插入key是否相同
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        break;
                    p = e;
                }
            }
            // 表示在桶中找到key值、hash值与插入元素相等的结点
            if (e != null) { // existing mapping for key
                V oldValue = e.value;
                //onlyIfAbsent flase 或者旧值 为null
                if (!onlyIfAbsent || oldValue == null)
                    // 替换
                    e.value = value;
                  // 用于LinkedHashMap的回调方法，HashMap为空实现
                afterNodeAccess(e);
                //返回旧值
                return oldValue;
            }
        }
        //结构修改
        ++modCount;
        //扩容
        if (++size > threshold)
            resize();
        //插入后回调
        afterNodeInsertion(evict);
        return null;
    }

//获取相应的节点
    final Node<K,V> getNode(int hash, Object key) {
       Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
       if ((tab = table) != null && (n = tab.length) > 0 &&
           (first = tab[(n - 1) & hash]) != null) {
           if (first.hash == hash && // always check first node
               ((k = first.key) == key || (key != null && key.equals(k))))
               return first;
           if ((e = first.next) != null) {
               if (first instanceof TreeNode)
                   return ((TreeNode<K,V>)first).getTreeNode(hash, key);
               do {
                   if (e.hash == hash &&
                       ((k = e.key) == key || (key != null && key.equals(k))))
                       return e;
               } while ((e = e.next) != null);
           }
       }
       return null;
   }
   //链表转红黑树
   final void treeifyBin(Node<K,V>[] tab, int hash) {
           int n, index; Node<K,V> e;
           //tab 初始化
           if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
               //扩容
               resize();
           //n = tab.length  判断tab节点是否为空
           else if ((e = tab[index = (n - 1) & hash]) != null) {
               TreeNode<K,V> hd = null, tl = null;
               do {
                   //node节点转treenode
                   TreeNode<K,V> p = replacementTreeNode(e, null);
                   if (tl == null)
                       hd = p;
                   else {
                       p.prev = tl;
                       tl.next = p;
                   }
                   tl = p;
               } while ((e = e.next) != null);
               //变为双链表（node节点为treenode）
               if ((tab[index] = hd) != null)
                   //转换为tree
                   hd.treeify(tab);
           }
       }

       //建立树 这个方法为treenode的内部类所以this为TreeNode
       final void treeify(Node<K,V>[] tab) {
             TreeNode<K,V> root = null;
             for (TreeNode<K,V> x = this, next; x != null; x = next) {
                 //获取当前节点的下一个节点
                 next = (TreeNode<K,V>)x.next;
                 x.left = x.right = null;
                 //初始化root节点
                 if (root == null) {
                     x.parent = null;
                     //红黑树根节点为黑色
                     x.red = false;
                     root = x;
                 }
                 else {
                     //暂存key与hash
                     K k = x.key;
                     int h = x.hash;
                     //?
                     Class<?> kc = null;
                     //从根节点开始查找位置
                     for (TreeNode<K,V> p = root;;) {
                         int dir, ph;
                         K pk = p.key;
                         if ((ph = p.hash) > h)
                             dir = -1;//左子树
                         else if (ph < h)
                             dir = 1;//右子树
                        //hash值相同继续判断
                         else if ((kc == null &&
                                   (kc = comparableClassFor(k)) == null) ||
                                  (dir = compareComparables(kc, k, pk)) == 0)
                             dir = tieBreakOrder(k, pk);

                         TreeNode<K,V> xp = p;
                         //确定插入位置
                         if ((p = (dir <= 0) ? p.left : p.right) == null) {
                             //设置父节点
                             x.parent = xp;
                             if (dir <= 0)
                                 xp.left = x;
                             else
                                 xp.right = x;
                             //修复红黑树
                             root = balanceInsertion(root, x);
                             break;
                         }
                     }
                 }
             }
             //移动到相应的节点上
             moveRootToFront(tab, root);
         }


         static <K,V> TreeNode<K,V> balanceInsertion(TreeNode<K,V> root,
                                                           TreeNode<K,V> x) {
               x.red = true;//插入的节点首先涂成红色，保证第四五条规则
                //xp 父节点 xpp 祖父节点 xppl 左兄弟 xppr 右兄弟
               for (TreeNode<K,V> xp, xpp, xppl, xppr;;) {
                   if ((xp = x.parent) == null) {//说明当前树是空
                       x.red = false;//根节点必须黑色
                       return x;//插入的节点直接就是根节点
                   }
                   else if (!xp.red || (xpp = xp.parent) == null)
                       return root;//父节点不是红色，那就不会违反红黑树规则，直接返回原本的根节点
                   if (xp == (xppl = xpp.left)) {//父亲节点是祖父节点的左孩子
                       if ((xppr = xpp.right) != null && xppr.red) {//如果叔叔是红色
                           xppr.red = false;//把叔叔节点涂黑
                           xp.red = false;//把父亲节点涂黑
                           xpp.red = true;//祖父节点涂红
                           x = xpp;//祖父节点作为“插入节点”进入下一次循环
                       }
                       else {//叔叔节点是黑色
                           if (x == xp.right) {//插入节点是父节点的右孩子
         				//以父亲节点为支点左旋，然后更新插入节点为父亲节点【标记5】
                               root = rotateLeft(root, x = xp);
         				//更新父节点和祖父节点
                               xpp = (xp = x.parent) == null ? null : xp.parent;
                           }
         			    //到这里插入节点一定是父节点的左孩子因为进行了左旋
                           if (xp != null) {
                               xp.red = false;//把父节点设置为黑
                               if (xpp != null) {
                                   xpp.red = true;//祖父节点涂成红色
                                   root = rotateRight(root, xpp);//【标记5】以祖父节点为支点右旋
                               }
                           }
                       }
                   }
                   else {
                        if (x == xp.left) {
                            root = rotateRight(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateLeft(root, xpp);
                            }
                        }
                    }
               }
           }


           //root是旧的根节点，p是旋转支点
           static <K,V> TreeNode<K,V> rotateLeft(TreeNode<K,V> root,
                                                 TreeNode<K,V> p) {
               TreeNode<K,V> r, pp, rl;
               if (p != null && (r = p.right) != null) {
                   if ((rl = p.right = r.left) != null)
                       rl.parent = p;//p的右节点的左孩子会变成p的右孩子
                   if ((pp = r.parent = p.parent) == null)//说明p原来是根节点
                       (root = r).red = false;//p的右孩子会变成根节点
                   else if (pp.left == p)
                       pp.left = r;//p原先是个左孩子
                   else
                       pp.right = r;//p原先是个右孩子
                   r.left = p;//p变成原先p的右节点的左孩子
                   p.parent = r;
               }
               return root;
           }

           static <K,V> void moveRootToFront(Node<K,V>[] tab, TreeNode<K,V> root) {
                int n;
                if (root != null && tab != null && (n = tab.length) > 0) {
                    int index = (n - 1) & root.hash;
                    TreeNode<K,V> first = (TreeNode<K,V>)tab[index];
                    if (root != first) {//root节点不是第一个节点
                        Node<K,V> rn;
                        tab[index] = root;
                        TreeNode<K,V> rp = root.prev;//前节点
                        if ((rn = root.next) != null)//
                            ((TreeNode<K,V>)rn).prev = rp;
                        if (rp != null)
                            rp.next = rn;
           			//上面就是删除链表节点操作
           			//下面就是把节点插到链表头操作
                        if (first != null)
                            first.prev = root;
                        root.next = first;
                        root.prev = null;
                    }
           		//检查状态是否正确
                    assert checkInvariants(root);
                }
            }
